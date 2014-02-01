#pragma version(1) 
#pragma rs java_package_name(com.arekolek.onioncamera)

// use of globals to operate on whole image (GetBorder and SimpleBlur)
rs_allocation gIn;
rs_allocation gTmp;
rs_allocation gOut;
rs_script gScript;
int width;
int height;

void init () 
{ 
    // optional function
    // executed once when setting up the script;
} 

uchar4 __attribute__((kernel)) Invert(uchar in, uint32_t x, uint32_t y) {
    uchar4 out;
    out.r = 255 - in;
    out.g = 255 - in;
    out.b = 255 - in;
    return out;
}

// separable function: performances could improve greatly implementing 
// this as a horizontal+vertical pass filter (next time!)
void GetBorder(const uchar *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    const float k1 = 0.145833f;
    const float k2 = 0.104167f;
    
    if ((x==0)||(x==width-1)||(y==0)||(y==height-1)) return;

    // elementXY is the element of a 3x3 matrix centered around current sample
    // TODO: learn about rsMatrixMultiply and alike
    const uchar *e11 = rsGetElementAt(gIn, x-1, y-1);
    const uchar *e21 = rsGetElementAt(gIn, x, y-1);
    const uchar *e31 = rsGetElementAt(gIn, x+1, y-1);

    const uchar *e12 = rsGetElementAt(gIn, x-1, y);
    const uchar *e22 = rsGetElementAt(gIn, x, y);
    const uchar *e32 = rsGetElementAt(gIn, x+1, y);

    const uchar *e13 = rsGetElementAt(gIn, x-1, y+1);
    const uchar *e23 = rsGetElementAt(gIn, x, y+1);
    const uchar *e33 = rsGetElementAt(gIn, x+1, y+1);

    uchar res = (uchar)clamp((int)((abs(*e21 - *e22)+abs(*e12 - *e22)+abs(*e32 - *e22)+abs(*e23 - *e22))*k1 + 
                (abs(*e11 - *e22)+abs(*e31 - *e22)+abs(*e13 - *e22)+abs(*e33 - *e22))*k2)*2,(int)0,(int)255);

    uchar4 res4;

    res4.r = res;
    res4.g = res;
    res4.b = res;
    res4.a = res;

    *v_out = res4;
}

// separable function: performances could improve greatly implementing 
// this as a horizontal+vertical pass filter (next time!
void SimpleBlur(const uchar *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    const float k1 = 0.1715728f; // w = 2
    const float k2 = 0.0857864f; // w = 1
    const float k3 = 0.0606601f; // w = 1/1.4 = 0.7

    if ((x==0)||(x==width-1)||(y==0)||(y==height-1)) return;

    // elementXY is the element of a 3x3 matrix centered around current sample
    // TODO: learn about rsMatrixMultiply and alike
    const uchar *e11 = rsGetElementAt(gIn, x-1, y-1);
    const uchar *e21 = rsGetElementAt(gIn, x, y-1);
    const uchar *e31 = rsGetElementAt(gIn, x+1, y-1);

    const uchar *e12 = rsGetElementAt(gIn, x-1, y);
    const uchar *e22 = rsGetElementAt(gIn, x, y); 
    const uchar *e32 = rsGetElementAt(gIn, x+1, y);

    const uchar *e13 = rsGetElementAt(gIn, x-1, y+1);
    const uchar *e23 = rsGetElementAt(gIn, x, y+1);
    const uchar *e33 = rsGetElementAt(gIn, x+1, y+1);

    uchar res = (uchar)( *e22 * k1 +
                (*e21 + *e12  + *e32  + *e23) * k2 + 
                (*e11 + *e31  + *e13 + *e33) * k3);

    uchar4 res4;

    res4.r = res;
    res4.g = res;
    res4.b = res;
    res4.a = res;

    *v_out = res4;
}


// follows the implementation of a separated Sobel filter
//
// 1 0 -1
// 2 0 -2
// 1 0 -1
//
void SobelFirstPass(const uchar *v_in, float *v_out, const void *usrData, uint32_t x, uint32_t y) 
{
    const uchar *e11 = rsGetElementAt(gIn, clamp((float)x-1,0.0f,(float)width), clamp((float)y - 1, 0.0f, (float)height)); 
    const uchar *e12 = rsGetElementAt(gIn, clamp((float)x-1,0.0f,(float)width), y); 
    const uchar *e13 = rsGetElementAt(gIn, clamp((float)x-1,0.0f,(float)width), clamp((float)y + 1, 0.0f, (float)height));
    
    const uchar *e31 = rsGetElementAt(gIn, clamp((float)x+1,0.0f,(float)width), clamp((float)y - 1, 0.0f, (float)height)); 
    const uchar *e32 = rsGetElementAt(gIn, clamp((float)x+1,0.0f,(float)width), y); 
    const uchar *e33 = rsGetElementAt(gIn, clamp((float)x+1,0.0f,(float)width), clamp((float)y + 1, 0.0f, (float)height));

    *v_out =(*e12 - *e32)*2 + *e11 + *e13 - *e31 - *e33;
}

void SobelSecondPass(const uchar *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) 
{
    const uchar *e11 = rsGetElementAt(gIn, clamp((float)x-1,0.0f,(float)width), clamp((float)y - 1, 0.0f, (float)height)); 
    const uchar *e21 = rsGetElementAt(gIn, x, clamp((float)y - 1, 0.0f, (float)height)); 
    const uchar *e31 = rsGetElementAt(gIn, clamp((float)x+1,0.0f,(float)width), clamp((float)y - 1, 0.0f, (float)height));
    
    const uchar *e13 = rsGetElementAt(gIn, clamp((float)x-1,0.0f,(float)width), clamp((float)y + 1, 0.0f, (float)height)); 
    const uchar *e23 = rsGetElementAt(gIn, x, clamp((float)y + 1, 0.0f, (float)height)); 
    const uchar *e33 = rsGetElementAt(gIn, clamp((float)x+1,0.0f,(float)width), clamp((float)y + 1, 0.0f, (float)height));

    const float *lastPassResult = rsGetElementAt(gTmp, x, y);

    uchar res =  (uchar)clamp((*e21 - *e23)*2 + *e11 - *e13 + *e31 - *e33 + *lastPassResult, 0.0f, 255.0f);
    
    *v_out = (uchar4){res, res, res, 255};
}