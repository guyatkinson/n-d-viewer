/*
 * Copyright (c) 1995-1998 Sun Microsystems, Inc. All Rights Reserved.
 *   Based on @(#)ThreeD.java	1.8 98/06/29.
 * Copyright (c) 2000-2013 Guy H Atkinson
 */

/* To do: stereoscopic, auto-scaling, auto-rotate, reduce flicker (double-buff)
       support rotating in more dimensions    
*/

/* A set of classes to parse, represent and display ND wireframe models
   represented in .obn format (based on Wavefront .obj format, which
   is still supported). */

import java.applet.Applet;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Event;
import java.awt.event.*;
import java.io.*;
import java.net.URL;

class FileFormatException extends Exception {
    public FileFormatException(String s) {
	super(s);
    }
}

/** The representation of an ND model */
class ModelND
{
    float vert[];
    int tvert[];
    int nvert, maxvert;
    int con[], conColour[];
    int ncon, maxcon;
    boolean transformed;
    MatrixND mat;
    int n = 3;
    boolean inColour = false;
    float min[], max[];

    ModelND ()
    {
    }
    /** Create an ND model by parsing an input stream */
    ModelND (InputStream is) throws IOException, FileFormatException {
      this();
      StreamTokenizer st = new StreamTokenizer(new BufferedReader(new InputStreamReader(is)));
      st.eolIsSignificant(true);
      st.commentChar('#');
    scan:
	while (true) {
	    switch (st.nextToken()) {
	      default:
		break scan;
	      case StreamTokenizer.TT_EOL:
		break;
	      case StreamTokenizer.TT_WORD:
		if ("v".equals(st.sval)) {
		    float a[] = new float[n];
		    for (int i=0; i<n; i++)
		    {
			if (st.nextToken() == StreamTokenizer.TT_NUMBER)
			{
			    a[i] = (float) st.nval;
			}
		    }
		    addVert(a);
		    while (st.ttype != StreamTokenizer.TT_EOL &&
			    st.ttype != StreamTokenizer.TT_EOF)
			st.nextToken();
		} else if ("f".equals(st.sval) || "fo".equals(st.sval) || "l".equals(st.sval)) {
		    int start = -1;
		    int prev = -1;
		    int n = -1;
		    while (true)
			if (st.nextToken() == StreamTokenizer.TT_NUMBER) {
			    n = (int) st.nval;
			    if (prev >= 0)
				add(prev - 1, n - 1);
			    if (start < 0)
				start = n;
			    prev = n;
			} else if (st.ttype == '/')
			    st.nextToken();
			else
			    break;
		    if (start >= 0)
			add(start - 1, prev - 1);
		    if (st.ttype != StreamTokenizer.TT_EOL)
			break scan;
		} else if ("d".equals(st.sval)) /* num dims (must be 1st non-comment line) */
                {
		    if (st.nextToken() == StreamTokenizer.TT_NUMBER)
                    {
		        n = (int) st.nval;
                    }
		} else if ("c".equals(st.sval)) /* colour */
                {
		    // Under Netscape, loading a new model even on a new page
		    //  (without shift Reload) keeps the same data incl colour[]
		    if (colour == null || colour.length < n)
		    { // NB "d 3" line may be absent for .obj
	   if (colour != null) { System.out.println("old c l="+colour.length); }
			colour = new Color [n];
System.out.println("new c n="+n);
                    }
		    int i, r, g, b;
		    if (st.nextToken() == StreamTokenizer.TT_NUMBER)
                    {
		      i = (int) st.nval;
		      if (st.nextToken() == StreamTokenizer.TT_NUMBER)
                      {
		        r = (int) st.nval;
		        if (st.nextToken() == StreamTokenizer.TT_NUMBER)
                        {
		          g = (int) st.nval;
		          if (st.nextToken() == StreamTokenizer.TT_NUMBER)
                          {
			    b = (int) st.nval;
//System.out.println("c i"+i);
			    colour[i] = new Color(r, g, b);
			    inColour = true;
                          }
                        }
                      }
                    }
		} else {
		    while (st.nextToken() != StreamTokenizer.TT_EOL
			    && st.ttype != StreamTokenizer.TT_EOF);
		}
	    }
	}
	is.close();
	if (st.ttype != StreamTokenizer.TT_EOF)
	    throw new FileFormatException(st.toString());
	mat = new MatrixND (n);
    }

    /** Find max dim in which line p1-p2 has non-zero presence **/
    int maxDim(int p1, int p2)
    {
	int j = 0;
        for (int i=0; i<n; i++)
	{
	    if (Math.abs(vert[p1*n+i]-vert[p2*n+i]) > 0.0001)
	    {
		j = i;
	    }
	}
	return j;
    }

    /** Add a vertex to this model */
    int addVert(float fa[])
    {
	int i = nvert;
	/*System.out.print("addVert n="+n+" i="+i+" mv="+maxvert+" nv="+nvert);*/
	/* see 2 lines below for println */
	if (i >= maxvert)
	    if (vert == null) {
		maxvert = 100;
		vert = new float[maxvert * n];
	    } else {
		maxvert *= 2;
		float nv[] = new float[maxvert * n];
		System.arraycopy(vert, 0, nv, 0, vert.length);
		vert = nv;
	    }
	i *= n;
        for (int j=0; j<n; j++)
        {
	    vert[i + j] = fa[j];
            /*System.out.print(" "+fa[j]); */
        }
	/*System.out.println();*/
	return nvert++;
    }
    /** Add a line from vertex p1 to vertex p2 */
    void add(int p1, int p2) {
	/*System.out.println("add p1="+p1+" p2="+p2+" nv="+nvert);*/ 
	int i = ncon;
	if (p1 >= nvert || p2 >= nvert)
	    return;
	if (i >= maxcon)
	    if (con == null) {
		maxcon = 100;
		con = new int[maxcon];
		conColour = new int[maxcon];
	    } else {
		maxcon *= 2;
		int nv[] = new int[maxcon];
		System.arraycopy(con, 0, nv, 0, con.length);
		con = nv;
		int nc[] = new int[maxcon];
		System.arraycopy(conColour, 0, nc, 0, conColour.length);
		conColour = nc;
	    }
	if (p1 > p2) {
	    int t = p1;
	    p1 = p2;
	    p2 = t;
	}
	con[i] = (p1 << 16) | p2;
	conColour[i] = maxDim(p1, p2); /* use dims of p1-p2 to spec colour */
//System.out.println("col i="+i+" p1="+p1+" p2="+p2+" j="+conColour[i]);
	ncon = i + 1;
    }
    /** Transform all the points in this model */
    void transform() {
	if (transformed || nvert <= 0)
	    return;
	if (tvert == null || tvert.length < nvert * n)
	    tvert = new int[nvert*n];
	mat.transform(vert, tvert, nvert);
	transformed = true;
        /*System.out.println(mat.toString());*/
    }

   /* Quick Sort implementation
    */
   private void quickSort(int a[], int left, int right)
   {
      int leftIndex = left;
      int rightIndex = right;
      int partionElement;
      if ( right > left)
      {

         /* Arbitrarily establishing partition element as the midpoint of
          * the array.
          */
         partionElement = a[ ( left + right ) / 2 ];

         // loop through the array until indices cross
         while( leftIndex <= rightIndex )
         {
            /* find the first element that is greater than or equal to
             * the partionElement starting from the leftIndex.
             */
            while( ( leftIndex < right ) && ( a[leftIndex] < partionElement ) )
               ++leftIndex;

            /* find an element that is smaller than or equal to
             * the partionElement starting from the rightIndex.
             */
            while( ( rightIndex > left ) &&
                   ( a[rightIndex] > partionElement ) )
               --rightIndex;

            // if the indexes have not crossed, swap
            if( leftIndex <= rightIndex )
            {
               swap(a, leftIndex, rightIndex);
               ++leftIndex;
               --rightIndex;
            }
         }

         /* If the right index has not reached the left side of array
          * must now sort the left partition.
          */
         if( left < rightIndex )
            quickSort( a, left, rightIndex );

         /* If the left index has not reached the right side of array
          * must now sort the right partition.
          */
         if( leftIndex < right )
            quickSort( a, leftIndex, right );

      }
   }

   private void swap(int a[], int i, int j)
   {
      int T;
      T = a[i];
      a[i] = a[j];
      a[j] = T;
   }


    /** eliminate duplicate lines */
    void compress() {
	int limit = ncon;
	int c[] = con;
	quickSort(con, 0, ncon - 1);
	int d = 0;
	int pp1 = -1;
	for (int i = 0; i < limit; i++) {
	    int p1 = c[i];
	    if (pp1 != p1) {
		c[d] = p1;
		conColour[d] = maxDim((p1>>16)&0xFFFF, p1&0xFFFF);
		d++;
	    }
	    pp1 = p1;
	}
	ncon = d;
    }

    static Color gr[];
    static Color colour[];


    /** Paint this model to a graphics context.  It uses the matrix associated
	with this model to map from model space to screen space.
	The next version of the browser should have double buffering,
	which will make this *much* nicer */
    void paint(Graphics g) {
	if (vert == null || nvert <= 0)
	    return;
	transform();
	if (gr == null) {
	    gr = new Color[16];
	    for (int i = 0; i < 16; i++) {
		int grey = (int) (170*(1-Math.pow(i/15.0, 2.3)));
		gr[i] = new Color(grey, grey, grey);
	    }
	}
	int lg = 0;
	int lim = ncon;
	int c[] = con;
	int v[] = tvert;
	if (lim <= 0 || nvert <= 0)
	    return;
	for (int i = 0; i < lim; i++) {
	    int T = c[i];
	    int p1 = ((T >> 16) & 0xFFFF) * n;
	    int p2 = (T & 0xFFFF) * n;
//System.out.println("i="+i+" p1="+p1/n+" p2="+p2/n+" c="+conColour[i]+" ic="+inColour);
	  if (inColour)
	  {
	    g.setColor(colour[conColour[i]]);
	  } else
	  {
	    int grey = v[p1 + 2] + v[p2 + 2];
	    if (grey < 0)
		grey = 0;
	    if (grey > 15)
		grey = 15;
	    if (grey != lg) {
		lg = grey;
		g.setColor(gr[grey]);
	    }
	  }
	    g.drawLine(v[p1], v[p1 + 1],
		       v[p2], v[p2 + 1]);
	/*System.out.println("draw "+p1+":"+v[p1]+","+v[p1 + 1]+" "+p2+":"+v[p2]+","+v[p2 + 1]);*/
	}
    }

    /** Find the bounding box of this model */
    void findBB()
    {
	if (nvert <= 0)
	    return;
	/*System.out.println("findBB n="+n+" v.l="+vert.length+" n="+n+" nv="+nvert);*/
	float v[] = vert;
	float min[], max[];
        min = new float[n];
        max = new float[n];
        for (int i=0; i<n; i++)
        {
            min[i] = v[i];
            max[i] = v[i];
        }
	for (int i = nvert * n; (i -= n) > 0;)
        {
            for (int j=0; j<n; j++)
            {
	        float t = v[i + j];
	        if (t < min[j])
	    	    min[j] = t;
	        if (t > max[j])
		    max[j] = t;
            }
	}
	this.min = min; /* prob ineff on heap; use System.arraycopy or use this.min&max directly*/
	this.max = max;
        /* for (int i=0; i<n; i++)
        {
	    this.min[i] = min[i];
	    this.max[i] = max[i];
        } */
    }
}  /* class ModelND */

/** An applet to put an ND model into a page */
public class ND extends DoubleBufferApplet
  implements Runnable, MouseListener, MouseMotionListener
  {
    ModelND md;
    boolean painted = true;
    float xfac;
    int prevx, prevy;
    float xtheta, ytheta;
    float scalefudge = 1;
    MatrixND amat, tmat;
    String mdname = null;
    String message = null;

    public void init() {
	mdname = getParameter("model");
	if (mdname == null)
	    mdname = "model.obj";
	System.out.println("init m="+mdname);
	try {
	    scalefudge = Float.valueOf(getParameter("scale")).floatValue();
	}catch(Exception e){};
	resize(getSize().width <= 20 ? 400 : getSize().width,
	       getSize().height <= 20 ? 400 : getSize().height);
	addMouseListener(this);
	addMouseMotionListener(this);
    }

    public void destroy() {
        removeMouseListener(this);
        removeMouseMotionListener(this);
    }

    public void run() {
	System.out.println("run");
	InputStream is = null;
	try {
	System.out.println("run 1");
	    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
	    is = new URL(getDocumentBase(), mdname).openStream();
	    ModelND m = new ModelND (is);
	    md = m;
	System.out.println("run bef rotate n="+m.n+" nv="+m.nvert);
	    amat = new MatrixND(md.n); tmat = new MatrixND(md.n);
	    amat.rotate(0, 2, 20);
	    amat.rotate(1, 2, 20);
	    for (int i=4; i<=m.n; i++)
	    {
		amat.rotate(0, i-1, 30);
		amat.rotate(1, i-1, 35);
		amat.rotate(2, i-1, 40);
	    }
	    m.findBB();
	    m.compress();
            float w=0;
            for (int i=0; i<m.n; i++)
            {
	        float wt = m.max[i] - m.min[i];
                if (w < wt)
                    w = wt;  /* max */
            }
	    float f1 = getSize().width  / w;
	    float f2 = getSize().height / w;
	    xfac =  (f1 < f2 ? f1 : f2) * scalefudge / (float)Math.sqrt(m.n);
	} catch(Exception e) {
	    md = null;
	    message = e.toString();
            e.printStackTrace();
	}
	try {
	    if (is != null)
		is.close();
	} catch(Exception e) {
	}
	repaint();
    }

    public void start() {
	System.out.println("start");
	if (md == null && message == null)
	    new Thread(this).start();
    }

    public void stop() {
	System.out.println("stop");
    }

    public  void mouseClicked(MouseEvent e) {
    }

    public  void mousePressed(MouseEvent e) {
        prevx = e.getX();
        prevy = e.getY();
        e.consume();
    }

    public  void mouseReleased(MouseEvent e) {
    }

    public  void mouseEntered(MouseEvent e) {
    }

    public  void mouseExited(MouseEvent e) {
    }

    public  void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
	int flags = (e.getModifiers() &(Event.SHIFT_MASK|Event.CTRL_MASK|Event.META_MASK|Event.ALT_MASK));
	/* Shift=1; Ctrl=2; Meta=right=4; Alt=middle=8 */
	/*System.out.println("md fl="+flags+","+(flags*2+1)%md.n+","+(flags*2)%md.n);*/

        tmat.unit();
        float xtheta = (prevy - y) * 360.0f / getSize().width;
        float ytheta = (x - prevx) * 360.0f / getSize().height;
        tmat.rotate((flags*2+1)%md.n, 2, xtheta);   /* mouse -> rotates in N-D */
        tmat.rotate((flags*2)%md.n,   2, ytheta); 
        amat.mult(tmat);
        if (painted) {
            painted = false;
            repaint();
        }
        prevx = x;
        prevy = y;
        e.consume();
    }

    public  void mouseMoved(MouseEvent e) {
    }

    public void paint(Graphics g) {
	if (md != null) {
	    float t[] = new float [md.n];
	    md.mat.unit();
	    for (int i=0; i<md.n; i++)
	    {
// Under Netscape, loading new a new page & model (partic if go to lower n)
//  can cause md.min==null (but not md.max!) which can =>
//  java.lang.NullPointerException: trying to access array element
//  on the line t[i] = ... . These 3 ifs stop the exception!!
//  findBB is presumably not being called before paint, probably
//  because the code/thread calling paint is still running - may
//  need to do proper closedown (stop is called irrespective of md.min==null).
//  Trace findBB, paint
if(t==null)       { System.out.println("t==null"); }
if(md.min==null)  { System.out.println("md.min==null"); }
if(md.max==null)  { System.out.println("md.max==null"); }
		t[i] = -(md.min[i] + md.max[i]) / 2;
	    }
	    md.mat.translate(t);
	    md.mat.mult(amat);
	    for (int i=0; i<md.n; i++)
	    {
		t[i] = xfac;
	    }
	    t[1] = -xfac;
	    t[2] = 16 * xfac / getSize().width;
	    md.mat.scale(t);
	    for (int i=0; i<md.n; i++)
	    {
		t[i] = 0;
	    }
	    t[0]=getSize().width/2; t[1]=getSize().height/2; t[2]=8; 
	    if (md.n>=4) { t[3] = 8; } 
	    md.mat.translate(t);
	    md.transformed = false;
	    md.paint(g);
	    setPainted();
	} else if (message != null) {
	    g.drawString("Error in model:", 3, 20);
	    g.drawString(message, 10, 40);
	}
    }

    private synchronized void setPainted() {
	painted = true;
	notifyAll();
    }
//    private synchronized void waitPainted() {
//	while (!painted)
//	    wait();
//	painted = false;
//    }

    public String getAppletInfo() {
        return "Title: ND \nAuthor: Guy Atkinson\nAn applet to put an ND model onto the screen.";
    }

    public String[][] getParameterInfo() {
        String[][] info = {
            {"model", "path string", "The path to the model to be displayed."},
            {"scale", "float", "The scale of the model.  Default is 1."}
        };
        return info;
    }
}   /* class ND */

