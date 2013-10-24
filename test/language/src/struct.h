struct A {unsigned char a; unsigned int b; long c;};
struct C {unsigned char a[3]; struct A c; union D {unsigned char i; float j;} d;};
