
/** An N-D matrix object that can transform sets of ND points
 and perform a variety of manipulations on the transform */
class MatrixND 
{
    /*final*/ int n;
    float a[][];
    static final double pi = Math.PI;

    /** Create a new unit matrix */
    MatrixND(int N) 
    {  /* set rest to 0.0 ? */
        n = N;
        a = new float [n][n+1];
        for (int i=0; i<n; i++)
        {
            a[i][i] = 1.0f;
        }
    }
    /** Scale by f in all dimensions */
    void scale(float f) 
    {
        for (int i=0; i<n; i++)
        {
            for (int j=0; j<=n; j++)
            {
                a[i][j] *= f;
            }
        }
    }
    /** Scale along each axis independently */
    void scale(float fa[])
    {
        for (int i=0; i<n; i++)
        {
            for (int j=0; j<=n; j++)
            {
                a[i][j] *= fa[i];
            }
        }
    }
    /** Translate the origin */
    void translate(float fa[])
    {
        for (int i=0; i<n; i++)
        {
            a[i][n] += fa[i];
        }
    }
    /** rotate theta degrees in the plane of dimensions d1 & d2 */
    void rotate(int d1, int d2, double theta)
    {
	theta *= (pi / 180);
	double ct = Math.cos(theta);
	double st = Math.sin(theta);
        float N[][] = new float[n][n+1];
        for (int j=0; j<=n; j++)
        {
            N[d1][j] = (float) (a[d1][j] * ct + a[d2][j] * st);
            N[d2][j] = (float) (a[d2][j] * ct - a[d1][j] * st);
        }
        for (int j=0; j<=n; j++)
        {
            a[d1][j] = N[d1][j];
            a[d2][j] = N[d2][j];
        }
    }
    /** Multiply this matrix by a second: M = M * R */
    void mult(MatrixND rhs)
    {
        float l[][] = new float[n][n+1];
        for (int i=0; i<n; i++)
        {
            for (int j=0; j<=n; j++)
            {
                l[i][j] = 0;
                for (int k=0; k<n; k++)
                {
                    l[i][j] += a[k][j] * rhs.a[i][k];
                }
            }
            l[i][n] += rhs.a[i][n];
        }
        /*     ij   kj       ik   kj       ik   kj       ik       in 
	float lxx = xx * rhs.xx + yx * rhs.xy + zx * rhs.xz;
	float lxy = xy * rhs.xx + yy * rhs.xy + zy * rhs.xz;
	float lxz = xz * rhs.xx + yz * rhs.xy + zz * rhs.xz;
	float lxo = xo * rhs.xx + yo * rhs.xy + zo * rhs.xz + rhs.xo; */

        for (int i=0; i<n; i++)
        {
            for (int j=0; j<=n; j++)
            {
                a[i][j] = l[i][j];
            }
        }
    }

    /** Reinitialize to the unit matrix */
    void unit()
    {
        for (int i=0; i<n; i++)
        {
            for (int j=0; j<=n; j++)
            {
                a[i][j] = 0;
            }
            a[i][i] = 1;
        }
    }
    /** Transform nvert points from v into tv.  v contains the input
        coordinates in floating point.  n successive entries in
	the array constitute a point.  tv ends up holding the transformed
	points as integers, n successive entries per point */
    void transform(float v[], int tv[], int nvert)
    {
        float l[][] = new float[n][n+1];
        float t[]   = new float[n];
        for (int i=0; i<n; i++)
        {
            for (int j=0; j<=n; j++)
            {
                l[i][j] = a[i][j];
            }
        }
	for (int i = nvert * n; (i -= n) >= 0;)
        {
            for (int j=0; j<n; j++)
            {
                t[j] = v[i + j];
            }
            for (int j=0; j<n; j++)
            {
	        int tvij = (int) (l[j][n]);
                for (int k=0; k<n; k++)
                {
	            tvij += (int) (t[k] * l[j][k]);
                }
	        tv[i + j] = tvij;
            }
	}
    }

    public String toString()
    {
        String s = "[";
        for (int i=0; i<n; i++)
        {
            for (int j=0; j<=n; j++)
            {
                s += a[i][j];
                if (j < n)
                    s += ",";
            }
            if (i < n-1)
                s += ";";
        }
        return (s + "]");
    }
}
