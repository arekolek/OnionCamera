#pragma version(1)
#pragma rs java_package_name(com.arekolek.onioncamera)
#pragma rs_fp_relaxed

static rs_allocation raw, magnitude, blurred, direction, candidates;
static float low, high;
static const uint32_t zero = 0;

void set_blur_input(rs_allocation u8_buf) {
	raw = u8_buf;
}

void set_compute_gradient_input(rs_allocation f_buf) {
	blurred = f_buf;
}

void set_suppress_input(rs_allocation f_buf, rs_allocation i_buf) {
	magnitude = f_buf;
	direction = i_buf;
}

void set_hysteresis_input(rs_allocation i_buf) {
	candidates = i_buf;
}

void set_thresholds(float l, float h) {
	low = l;
	high = h;
}

inline static float getElementAt_uchar_to_float(rs_allocation a, uint32_t x,
		uint32_t y) {
	return rsGetElementAt_uchar(a, x, y) / 255.0f;
}

static rs_allocation histogram;

void set_histogram(rs_allocation h) {
	histogram = h;
}

uchar4 __attribute__((kernel)) addhisto(uchar in, uint32_t x, uint32_t y) {
	int px = (x - 100) / 2;
	if (px > -1 && px < 256) {
		int v = log((float) rsGetElementAt_int(histogram, (uint32_t) px)) * 30;
		int py = (400 - y);
		if (py > -1 && v > py) {
			in = 255;
		}
		if (py == -1) {
			in = 255;
		}
	}
	uchar4 out = { in, in, in, 255 };
	return out;
}

uchar4 __attribute__((kernel)) copy(uchar in) {
	uchar4 out = { in, in, in, 255 };
	return out;
}

uchar4 __attribute__((kernel)) blend(uchar4 in, uint32_t x, uint32_t y) {
	uchar r = rsGetElementAt_uchar(raw, x, y);
	uchar4 out = { r, r, r, 255 };
	return max(out, in);
}

float __attribute__((kernel)) blur(uint32_t x, uint32_t y) {
	float pixel = 0;

	pixel += 2 * getElementAt_uchar_to_float(raw, x - 2, y - 2);
	pixel += 4 * getElementAt_uchar_to_float(raw, x - 1, y - 2);
	pixel += 5 * getElementAt_uchar_to_float(raw, x, y - 2);
	pixel += 4 * getElementAt_uchar_to_float(raw, x + 1, y - 2);
	pixel += 2 * getElementAt_uchar_to_float(raw, x + 2, y - 2);

	pixel += 4 * getElementAt_uchar_to_float(raw, x - 2, y - 1);
	pixel += 9 * getElementAt_uchar_to_float(raw, x - 1, y - 1);
	pixel += 12 * getElementAt_uchar_to_float(raw, x, y - 1);
	pixel += 9 * getElementAt_uchar_to_float(raw, x + 1, y - 1);
	pixel += 4 * getElementAt_uchar_to_float(raw, x + 2, y - 1);

	pixel += 5 * getElementAt_uchar_to_float(raw, x - 2, y);
	pixel += 12 * getElementAt_uchar_to_float(raw, x - 1, y);
	pixel += 15 * getElementAt_uchar_to_float(raw, x, y);
	pixel += 12 * getElementAt_uchar_to_float(raw, x + 1, y);
	pixel += 5 * getElementAt_uchar_to_float(raw, x + 2, y);

	pixel += 4 * getElementAt_uchar_to_float(raw, x - 2, y + 1);
	pixel += 9 * getElementAt_uchar_to_float(raw, x - 1, y + 1);
	pixel += 12 * getElementAt_uchar_to_float(raw, x, y + 1);
	pixel += 9 * getElementAt_uchar_to_float(raw, x + 1, y + 1);
	pixel += 4 * getElementAt_uchar_to_float(raw, x + 2, y + 1);

	pixel += 2 * getElementAt_uchar_to_float(raw, x - 2, y + 2);
	pixel += 4 * getElementAt_uchar_to_float(raw, x - 1, y + 2);
	pixel += 5 * getElementAt_uchar_to_float(raw, x, y + 2);
	pixel += 4 * getElementAt_uchar_to_float(raw, x + 1, y + 2);
	pixel += 2 * getElementAt_uchar_to_float(raw, x + 2, y + 2);

	pixel /= 159;

	return pixel;
}

float __attribute__((kernel)) compute_gradient(uint32_t x, uint32_t y) {
	float gx = 0;

	gx -= rsGetElementAt_float(blurred, x - 1, y - 1);
	gx -= rsGetElementAt_float(blurred, x - 1, y) * 2;
	gx -= rsGetElementAt_float(blurred, x - 1, y + 1);
	gx += rsGetElementAt_float(blurred, x + 1, y - 1);
	gx += rsGetElementAt_float(blurred, x + 1, y) * 2;
	gx += rsGetElementAt_float(blurred, x + 1, y + 1);

	float gy = 0;

	gy += rsGetElementAt_float(blurred, x - 1, y - 1);
	gy += rsGetElementAt_float(blurred, x, y - 1) * 2;
	gy += rsGetElementAt_float(blurred, x + 1, y - 1);
	gy -= rsGetElementAt_float(blurred, x - 1, y + 1);
	gy -= rsGetElementAt_float(blurred, x, y + 1) * 2;
	gy -= rsGetElementAt_float(blurred, x + 1, y + 1);

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

