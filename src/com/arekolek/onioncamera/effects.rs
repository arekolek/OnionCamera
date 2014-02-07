#pragma version(1)
#pragma rs java_package_name(com.arekolek.onioncamera)
#pragma rs_fp_relaxed

static rs_allocation in1, in2;
static uint32_t maxX, maxY;
static const uint32_t zero = 0;

void set_buffers(rs_allocation a, rs_allocation b) {
	in1 = a;
	in2 = b;
	maxX = rsAllocationGetDimX(a) - 1;
	maxY = rsAllocationGetDimY(a) - 1;
}

// TODO run only on a subset of the input (launch options)
// TODO no race conditions in histogram?

inline static uchar4 getElementAt_uchar4_clamped(rs_allocation a, uint32_t x,
		uint32_t y) {
	return rsGetElementAt_uchar4(a, clamp(x, zero, maxX), clamp(y, zero, maxY));
}

inline static float4 getElementAt_unpack(rs_allocation a, uint32_t x,
		uint32_t y) {
	return rsUnpackColor8888(getElementAt_uchar4_clamped(a, x, y));
}

uchar4 __attribute__((kernel)) blur(uint32_t x, uint32_t y) {
	float3 pixel = 0;

	pixel += getElementAt_unpack(in1, x - 2, y - 2).rgb * 2;
	pixel += getElementAt_unpack(in1, x - 1, y - 2).rgb * 4;
	pixel += getElementAt_unpack(in1, x, y - 2).rgb * 5;
	pixel += getElementAt_unpack(in1, x + 1, y - 2).rgb * 4;
	pixel += getElementAt_unpack(in1, x + 2, y - 2).rgb * 2;

	pixel += getElementAt_unpack(in1, x - 2, y - 1).rgb * 4;
	pixel += getElementAt_unpack(in1, x - 1, y - 1).rgb * 9;
	pixel += getElementAt_unpack(in1, x, y - 1).rgb * 12;
	pixel += getElementAt_unpack(in1, x + 1, y - 1).rgb * 9;
	pixel += getElementAt_unpack(in1, x + 2, y - 1).rgb * 4;

	pixel += getElementAt_unpack(in1, x - 2, y).rgb * 5;
	pixel += getElementAt_unpack(in1, x - 1, y).rgb * 12;
	pixel += getElementAt_unpack(in1, x, y).rgb * 15;
	pixel += getElementAt_unpack(in1, x + 1, y).rgb * 12;
	pixel += getElementAt_unpack(in1, x + 2, y).rgb * 5;

	pixel += getElementAt_unpack(in1, x - 2, y + 1).rgb * 4;
	pixel += getElementAt_unpack(in1, x - 1, y + 1).rgb * 9;
	pixel += getElementAt_unpack(in1, x, y + 1).rgb * 12;
	pixel += getElementAt_unpack(in1, x + 1, y + 1).rgb * 9;
	pixel += getElementAt_unpack(in1, x + 2, y + 1).rgb * 4;

	pixel += getElementAt_unpack(in1, x - 2, y + 2).rgb * 2;
	pixel += getElementAt_unpack(in1, x - 1, y + 2).rgb * 4;
	pixel += getElementAt_unpack(in1, x, y + 2).rgb * 5;
	pixel += getElementAt_unpack(in1, x + 1, y + 2).rgb * 4;
	pixel += getElementAt_unpack(in1, x + 2, y + 2).rgb * 2;

	pixel /= 159;

	return rsPackColorTo8888(pixel);
}

uchar4 __attribute__((kernel)) edges(uint32_t x, uint32_t y) {
	float3 pixel = getElementAt_unpack(in1, x, y).rgb * -4;

	pixel += getElementAt_unpack(in1, x, y - 1).rgb;
	pixel += getElementAt_unpack(in1, x - 1, y).rgb;
	pixel += getElementAt_unpack(in1, x, y + 1).rgb;
	pixel += getElementAt_unpack(in1, x + 1, y).rgb;

	return rsPackColorTo8888(pixel);
}

uchar4 __attribute__((kernel)) sobel(uint32_t x, uint32_t y) {
	float3 gx = 0;

	gx -= getElementAt_unpack(in2, x - 1, y - 1).rgb;
	gx -= getElementAt_unpack(in2, x - 1, y).rgb * 2;
	gx -= getElementAt_unpack(in2, x - 1, y + 1).rgb;
	gx += getElementAt_unpack(in2, x + 1, y - 1).rgb;
	gx += getElementAt_unpack(in2, x + 1, y).rgb * 2;
	gx += getElementAt_unpack(in2, x + 1, y + 1).rgb;

	float3 gy = 0;

	gx += getElementAt_unpack(in2, x - 1, y - 1).rgb;
	gx += getElementAt_unpack(in2, x, y - 1).rgb * 2;
	gx += getElementAt_unpack(in2, x + 1, y - 1).rgb;
	gx -= getElementAt_unpack(in2, x - 1, y + 1).rgb;
	gx -= getElementAt_unpack(in2, x, y + 1).rgb * 2;
	gx -= getElementAt_unpack(in2, x + 1, y + 1).rgb;

	float3 pixel = fabs(gx) + fabs(gy);

	return rsPackColorTo8888(pixel);
}
