#pragma version(1)
#pragma rs java_package_name(com.arekolek.onioncamera)
#pragma rs_fp_relaxed

static rs_allocation image, magnitude, buffer2, direction, candidates;
static float low, high;
static uint32_t maxX, maxY;
static const uint32_t zero = 0;

void set_buffers(rs_allocation out, rs_allocation tmp1, rs_allocation tmp2,
		rs_allocation dir, rs_allocation e) {
	image = out;
	magnitude = tmp1;
	buffer2 = tmp2;
	direction = dir;
	candidates = e;
	maxX = rsAllocationGetDimX(out) - 1;
	maxY = rsAllocationGetDimY(out) - 1;
}

void set_thresholds(float l, float h) {
	low = l;
	high = h;
}

inline static uchar4 getElementAt_uchar4_clamped(rs_allocation a, uint32_t x,
		uint32_t y) {
	return rsGetElementAt_uchar4(a, clamp(x, zero, maxX), clamp(y, zero, maxY));
}

inline static float getElementAt_float_clamped(rs_allocation a, uint32_t x,
		uint32_t y) {
	return rsGetElementAt_float(a, clamp(x, zero, maxX), clamp(y, zero, maxY));
}

inline static float4 getElementAt_unpack(rs_allocation a, uint32_t x,
		uint32_t y) {
	return rsUnpackColor8888(getElementAt_uchar4_clamped(a, x, y));
}

float __attribute__((kernel)) unpack(uchar4 in) {
	return rsUnpackColor8888(in).r;
}

uchar4 __attribute__((kernel)) pack(float in) {
	return rsPackColorTo8888((float3) in);
}

float __attribute__((kernel)) blur(uint32_t x, uint32_t y) {
	float pixel = 0;

	pixel += getElementAt_float_clamped(magnitude, x - 2, y - 2) * 2;
	pixel += getElementAt_float_clamped(magnitude, x - 1, y - 2) * 4;
	pixel += getElementAt_float_clamped(magnitude, x, y - 2) * 5;
	pixel += getElementAt_float_clamped(magnitude, x + 1, y - 2) * 4;
	pixel += getElementAt_float_clamped(magnitude, x + 2, y - 2) * 2;

	pixel += getElementAt_float_clamped(magnitude, x - 2, y - 1) * 4;
	pixel += getElementAt_float_clamped(magnitude, x - 1, y - 1) * 9;
	pixel += getElementAt_float_clamped(magnitude, x, y - 1) * 12;
	pixel += getElementAt_float_clamped(magnitude, x + 1, y - 1) * 9;
	pixel += getElementAt_float_clamped(magnitude, x + 2, y - 1) * 4;

	pixel += getElementAt_float_clamped(magnitude, x - 2, y) * 5;
	pixel += getElementAt_float_clamped(magnitude, x - 1, y) * 12;
	pixel += getElementAt_float_clamped(magnitude, x, y) * 15;
	pixel += getElementAt_float_clamped(magnitude, x + 1, y) * 12;
	pixel += getElementAt_float_clamped(magnitude, x + 2, y) * 5;

	pixel += getElementAt_float_clamped(magnitude, x - 2, y + 1) * 4;
	pixel += getElementAt_float_clamped(magnitude, x - 1, y + 1) * 9;
	pixel += getElementAt_float_clamped(magnitude, x, y + 1) * 12;
	pixel += getElementAt_float_clamped(magnitude, x + 1, y + 1) * 9;
	pixel += getElementAt_float_clamped(magnitude, x + 2, y + 1) * 4;

	pixel += getElementAt_float_clamped(magnitude, x - 2, y + 2) * 2;
	pixel += getElementAt_float_clamped(magnitude, x - 1, y + 2) * 4;
	pixel += getElementAt_float_clamped(magnitude, x, y + 2) * 5;
	pixel += getElementAt_float_clamped(magnitude, x + 1, y + 2) * 4;
	pixel += getElementAt_float_clamped(magnitude, x + 2, y + 2) * 2;

	pixel /= 159;

	return pixel;
}

float __attribute__((kernel)) edges(uint32_t x, uint32_t y) {
	float pixel = getElementAt_float_clamped(magnitude, x, y) * -4;

	pixel += getElementAt_float_clamped(magnitude, x, y - 1);
	pixel += getElementAt_float_clamped(magnitude, x - 1, y);
	pixel += getElementAt_float_clamped(magnitude, x, y + 1);
	pixel += getElementAt_float_clamped(magnitude, x + 1, y);

	return pixel;
}

float __attribute__((kernel)) compute_gradient(uint32_t x, uint32_t y) {
	float gx = 0;

	gx -= getElementAt_float_clamped(buffer2, x - 1, y - 1);
	gx -= getElementAt_float_clamped(buffer2, x - 1, y) * 2;
	gx -= getElementAt_float_clamped(buffer2, x - 1, y + 1);
	gx += getElementAt_float_clamped(buffer2, x + 1, y - 1);
	gx += getElementAt_float_clamped(buffer2, x + 1, y) * 2;
	gx += getElementAt_float_clamped(buffer2, x + 1, y + 1);

	float gy = 0;

	gy += getElementAt_float_clamped(buffer2, x - 1, y - 1);
	gy += getElementAt_float_clamped(buffer2, x, y - 1) * 2;
	gy += getElementAt_float_clamped(buffer2, x + 1, y - 1);
	gy -= getElementAt_float_clamped(buffer2, x - 1, y + 1);
	gy -= getElementAt_float_clamped(buffer2, x, y + 1) * 2;
	gy -= getElementAt_float_clamped(buffer2, x + 1, y + 1);

	int d = ((int) round(atan2pi(gy, gx) * 4.0f) + 4) % 4;
	rsSetElementAt_int(direction, d, x, y);
	return hypot(gx, gy);
}

int __attribute__((kernel)) suppress(uint32_t x, uint32_t y) {
	int d = rsGetElementAt_int(direction, x, y);
	float g = rsGetElementAt_float(magnitude, x, y);
	if (d == 0) {
		// horizontal, check left and right
		float a = rsGetElementAt_float(magnitude, x - 1, y);
		float b = rsGetElementAt_float(magnitude, x + 1, y);
		return a < g && b < g ? 1 : 0;
	} else if (d == 2) {
		// vertical, check above and below
		float a = rsGetElementAt_float(magnitude, x, y - 1);
		float b = rsGetElementAt_float(magnitude, x, y + 1);
		return a < g && b < g ? 1 : 0;
	} else if (d == 1) {
		// NW-SE
		float a = rsGetElementAt_float(magnitude, x - 1, y - 1);
		float b = rsGetElementAt_float(magnitude, x + 1, y + 1);
		return a < g && b < g ? 1 : 0;
	} else {
		// NE-SW
		float a = rsGetElementAt_float(magnitude, x + 1, y - 1);
		float b = rsGetElementAt_float(magnitude, x - 1, y + 1);
		return a < g && b < g ? 1 : 0;
	}
}

static const int NON_EDGE = 0b000;
static const int LOW_EDGE = 0b001;
static const int MED_EDGE = 0b010;
static const int HIG_EDGE = 0b100;

inline static int getEdgeType(uint32_t x, uint32_t y) {
	int e = rsGetElementAt_int(candidates, x, y);
	float g = rsGetElementAt_float(magnitude, x, y);
	if (e == 1) {
		if (g < low)
			return LOW_EDGE;
		if (g > high)
			return HIG_EDGE;
		return MED_EDGE;
	}
	return NON_EDGE;
}

uchar4 __attribute__((kernel)) hysteresis(uint32_t x, uint32_t y) {
	uchar4 white = { 255, 255, 255, 255 };
	uchar4 red = { 255, 0, 0, 255 };
	uchar4 black = { 0, 0, 0, 255 };
	int type = getEdgeType(x, y);
	if (type) {
		if (type & LOW_EDGE)
			return black;
		if (type & HIG_EDGE)
			return white;

		// it's medium, check nearest neighbours
		type = getEdgeType(x - 1, y - 1);
		type |= getEdgeType(x, y - 1);
		type |= getEdgeType(x + 1, y - 1);
		type |= getEdgeType(x - 1, y);
		type |= getEdgeType(x + 1, y);
		type |= getEdgeType(x - 1, y + 1);
		type |= getEdgeType(x, y + 1);
		type |= getEdgeType(x + 1, y + 1);

		if (type & HIG_EDGE)
			return white;

		if (type & MED_EDGE) {
			// check further
			type = getEdgeType(x - 2, y - 2);
			type |= getEdgeType(x - 1, y - 2);
			type |= getEdgeType(x, y - 2);
			type |= getEdgeType(x + 1, y - 2);
			type |= getEdgeType(x + 2, y - 2);
			type |= getEdgeType(x - 2, y - 1);
			type |= getEdgeType(x + 2, y - 1);
			type |= getEdgeType(x - 2, y);
			type |= getEdgeType(x + 2, y);
			type |= getEdgeType(x - 2, y + 1);
			type |= getEdgeType(x + 2, y + 1);
			type |= getEdgeType(x - 2, y + 2);
			type |= getEdgeType(x - 1, y + 2);
			type |= getEdgeType(x, y + 2);
			type |= getEdgeType(x + 1, y + 2);
			type |= getEdgeType(x + 2, y + 2);

			if (type & HIG_EDGE)
				return white;
		}
	}
	return black;
}

