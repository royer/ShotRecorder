package com.bangz.audio;

/**
 * Created by royer on 22/05/13.
 *
 * the toDB and ClipZeroToOne is port from audacity project.(http://audacity.sourceforge.net/) .
 *
 */
public class Sample {

    private float[] s ;

    public static float toDB(float v) {

        double db ;

        if (v > 0 ) {
            db = 20 * Math.log10(Math.abs(v)) ;
        } else
            db = 0.0 ;

        return (float)db;
    }

    public static float DBtoLinear(float db) {
    		
    	double linear ;
    	
    	linear = Math.pow(10.0, db / 20.0);
    	
    	if (linear > 1.0)
    		linear = 1.0f ; 
    	else if (linear < 0.0)
    		linear = 0.0f;
    	return (float)linear;
    }
    
    public static float ClipZeroToOne(float z) {

        if (z > 1.0f )
            return 1.0f ;
        else if (z < 0.0f)
            return 0.0f ;
        else
            return z;

    }
}
