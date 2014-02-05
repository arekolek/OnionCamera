
#pragma version(1)
#pragma rs java_package_name(com.arekolek.onioncamera)
#pragma rs_fp_relaxed

static rs_allocation in;
static int width, height;

void set_input(rs_allocation a) {
	in = a;
	width = rsAllocationGetDimX(a);
	height = rsAllocationGetDimY(a);
}

// TODO use rs_sampler instead? (supposedly it's worse)
// TODO bind allocation to array instead?
// TODO add rs_fp_relaxed
// TODO rsForeach in same file makes it nonthreadable?
// TODO run only on a subset of the input (launch options)
// TODO no race conditions in histogram?

inline static uint32_t clamp_to_x(uint32_t x, rs_allocation a) {
	return clamp(x, (uint32_t)0, rsAllocationGetDimX(a)-1);
}

inline static uint32_t clamp_to_y(uint32_t y, rs_allocation a) {
	return clamp(y, (uint32_t)0, rsAllocationGetDimY(a)-1);
}

inline static uchar4 getElementAt_uchar4_clamped(rs_allocation a, uint32_t x, uint32_t y) {
	return rsGetElementAt_uchar4(a, clamp_to_x(x, a), clamp_to_y(y, a));
}

inline static float4 getElementAt_unpack(rs_allocation a, uint32_t x, uint32_t y) {
	return rsUnpackColor8888(getElementAt_uchar4_clamped(a, x, y));
}

uchar4 __attribute__((kernel)) blur(uint32_t x, uint32_t y) {
    float3 blurred = getElementAt_unpack(in, x, y).rgb * 0.12f;
    
    int o = 6;
    
    blurred += getElementAt_unpack(in, x, y-o).rgb * 0.12f;
    blurred += getElementAt_unpack(in, x-o, y).rgb * 0.12f;
    blurred += getElementAt_unpack(in, x, y+o).rgb * 0.12f;
    blurred += getElementAt_unpack(in, x+o, y).rgb * 0.12f;
    
    o = 3;
    
    blurred += getElementAt_unpack(in, x-o, y-o).rgb * 0.1f;
    blurred += getElementAt_unpack(in, x+o, y-o).rgb * 0.1f;
    blurred += getElementAt_unpack(in, x+o, y+o).rgb * 0.1f;
    blurred += getElementAt_unpack(in, x-o, y+o).rgb * 0.1f;
    
    return rsPackColorTo8888(blurred);
}

uchar4 __attribute__((kernel)) edges(uint32_t x, uint32_t y) {
    float3 blurred = getElementAt_unpack(in, x, y).rgb * -1.0f;
    
    int o = 6;
    
    blurred += getElementAt_unpack(in, x, y-o).rgb * 0.25f;
    blurred += getElementAt_unpack(in, x-o, y).rgb * 0.25f;
    blurred += getElementAt_unpack(in, x, y+o).rgb * 0.25f;
    blurred += getElementAt_unpack(in, x+o, y).rgb * 0.25f;
    
    /*
	o = 3;
    
    blurred += getElementAt_unpack(in, x-o, y-o).rgb * 0.1f;
    blurred += getElementAt_unpack(in, x+o, y-o).rgb * 0.1f;
    blurred += getElementAt_unpack(in, x+o, y+o).rgb * 0.1f;
    blurred += getElementAt_unpack(in, x-o, y+o).rgb * 0.1f;
    */
    
    return rsPackColorTo8888(blurred);
}
