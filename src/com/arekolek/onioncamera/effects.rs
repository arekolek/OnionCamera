#pragma version(1)
#pragma rs java_package_name(com.arekolek.onioncamera)
#pragma rs_fp_relaxed

static rs_allocation image, buffer1, buffer2;
static uint32_t maxX, maxY;
static const uint32_t zero = 0;

void set_buffers(rs_allocation out, rs_allocation tmp1, rs_allocation tmp2) {
	image = out;
	buffer1 = tmp1;
	buffer2 = tmp2;
	maxX = rsAllocationGetDimX(out) - 1;
	maxY = rsAllocationGetDimY(out) - 1;
}

// TODO run only on a subset of the input (launch options)
// TODO no race conditions in histogram?

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
	//float out = {in, in, in, 1.0f};
	return rsPackColorTo8888((float3)in);
}

float __attribute__((kernel)) blur(uint32_t x, uint32_t y) {
	float pixel = 0;

	pixel += getElementAt_float_clamped(buffer1, x - 2, y - 2) * 2;
	pixel += getElementAt_float_clamped(buffer1, x - 1, y - 2) * 4;
	pixel += getElementAt_float_clamped(buffer1, x, y - 2) * 5;
	pixel += getElementAt_float_clamped(buffer1, x + 1, y - 2) * 4;
	pixel += getElementAt_float_clamped(buffer1, x + 2, y - 2) * 2;

	pixel += getElementAt_float_clamped(buffer1, x - 2, y - 1) * 4;
	pixel += getElementAt_float_clamped(buffer1, x - 1, y - 1) * 9;
	pixel += getElementAt_float_clamped(buffer1, x, y - 1) * 12;
	pixel += getElementAt_float_clamped(buffer1, x + 1, y - 1) * 9;
	pixel += getElementAt_float_clamped(buffer1, x + 2, y - 1) * 4;

	pixel += getElementAt_float_clamped(buffer1, x - 2, y) * 5;
	pixel += getElementAt_float_clamped(buffer1, x - 1, y) * 12;
	pixel += getElementAt_float_clamped(buffer1, x, y) * 15;
	pixel += getElementAt_float_clamped(buffer1, x + 1, y) * 12;
	pixel += getElementAt_float_clamped(buffer1, x + 2, y) * 5;

	pixel += getElementAt_float_clamped(buffer1, x - 2, y + 1) * 4;
	pixel += getElementAt_float_clamped(buffer1, x - 1, y + 1) * 9;
	pixel += getElementAt_float_clamped(buffer1, x, y + 1) * 12;
	pixel += getElementAt_float_clamped(buffer1, x + 1, y + 1) * 9;
	pixel += getElementAt_float_clamped(buffer1, x + 2, y + 1) * 4;

	pixel += getElementAt_float_clamped(buffer1, x - 2, y + 2) * 2;
	pixel += getElementAt_float_clamped(buffer1, x - 1, y + 2) * 4;
	pixel += getElementAt_float_clamped(buffer1, x, y + 2) * 5;
	pixel += getElementAt_float_clamped(buffer1, x + 1, y + 2) * 4;
	pixel += getElementAt_float_clamped(buffer1, x + 2, y + 2) * 2;

	pixel /= 159;

	return pixel;
}

float __attribute__((kernel)) edges(uint32_t x, uint32_t y) {
	float pixel = getElementAt_float_clamped(buffer1, x, y) * -4;

	pixel += getElementAt_float_clamped(buffer1, x, y - 1);
	pixel += getElementAt_float_clamped(buffer1, x - 1, y);
	pixel += getElementAt_float_clamped(buffer1, x, y + 1);
	pixel += getElementAt_float_clamped(buffer1, x + 1, y);

	return pixel;
}

float __attribute__((kernel)) sobel(uint32_t x, uint32_t y) {
	float gx = 0;

	gx -= getElementAt_float_clamped(buffer2, x - 1, y - 1);
	gx -= getElementAt_float_clamped(buffer2, x - 1, y) * 2;
	gx -= getElementAt_float_clamped(buffer2, x - 1, y + 1);
	gx += getElementAt_float_clamped(buffer2, x + 1, y - 1);
	gx += getElementAt_float_clamped(buffer2, x + 1, y) * 2;
	gx += getElementAt_float_clamped(buffer2, x + 1, y + 1);

	float gy = 0;

	gx += getElementAt_float_clamped(buffer2, x - 1, y - 1);
	gx += getElementAt_float_clamped(buffer2, x, y - 1) * 2;
	gx += getElementAt_float_clamped(buffer2, x + 1, y - 1);
	gx -= getElementAt_float_clamped(buffer2, x - 1, y + 1);
	gx -= getElementAt_float_clamped(buffer2, x, y + 1) * 2;
	gx -= getElementAt_float_clamped(buffer2, x + 1, y + 1);

	float pixel = fabs(gx) + fabs(gy);

	return pixel;
}
