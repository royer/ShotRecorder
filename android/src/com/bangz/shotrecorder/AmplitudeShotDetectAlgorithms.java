/**
 * Copyright (C) 2013 Bangz
 *
 * @author Royer Wang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.bangz.shotrecorder;

import com.bangz.audio.Sample;
import com.bangz.audio.SampleBuffer;

import java.util.ArrayList;

//import com.bangz.audio.SampleBuffer;

/**
 * Created by royer on 23/05/13.
 *
 * this is a simple and ugly algorithms. when detect a threshold dB, and flow 5 slice rms db should
 * be very close the threshold dB around decrease and not more than 0.02%.
 */
public class AmplitudeShotDetectAlgorithms  implements ShotDetector.ShotDetectAlgorithms {

    float mThresholdLinear ;


    @Override
    public Long[] DetectShotAlgorithms(SampleBuffer buffer, SampleBuffer prevBuffer) {


    	ArrayList<Long>		lresult = new ArrayList<Long>() ;
    	float[] a_sample = new float[buffer.getChannels()] ;
    	
    	
    	while(buffer.hasRemaining()) {
    		
    		buffer.get(a_sample) ;
    		
    		float fs = 0.0f;
    		for(float f : a_sample)
    			fs += f;
    		fs /= a_sample.length ;
    		
    		fs = Math.abs(fs) ;
    		
    		if (fs >= mThresholdLinear) {
    			
    			buffer.position(buffer.position()-buffer.getChannels()) ;
    			long currentsamples = buffer.position_of_sample();
    			
    			if ( buffer.remainingsamples() < getMinDetectRequireSlice()*buffer.CalculateTimeToSamples(getSliceTime()) ) {
    				break ;
    			}
    			
    			// start check each slice  rms is weather close thresholdDB and is it clam down?
    			double prevslicedb = CalculatePrevSliceDB(buffer, prevBuffer) ;
    			
    			if(CheckNextSlices(buffer,prevslicedb)) {
    				lresult.add(currentsamples);
    			}
    		}
    	}
    	

        return lresult.toArray(new Long[lresult.size()]);
    }

    double CalculatePrevSliceDB(SampleBuffer buffer, SampleBuffer prevBuffer) {
    	
    	int slicesamples = (int)buffer.CalculateTimeToSamples(getSliceTime());
    	
    	int position = buffer.position() - 1 ;
    	int i , count;
    	
    	double f = 0.0;
    	double s ;
    	count = 0;
    	if (position >= 0) {
	    	for(i = position; i >= 0 && count < slicesamples; i-=buffer.getChannels()) {
	    		s = 0.0 ;
	    		for(int j = 0; j< buffer.getChannels(); j++) {
	    			s += buffer.get(i-j);
	    		}
	    		s /= buffer.getChannels();
	    		f += s * s ;
	    		count++;
	    	}
    	}
    	
    	position = prevBuffer.limit()-1;
    	if (position >= 0) {
	    	for(i = position; i >= 0 && count < slicesamples; i-=prevBuffer.getChannels()) {
	    		s = 0.0;
	    		for(int j = 0; j < prevBuffer.getChannels(); j++) {
	    			s += prevBuffer.get(i-j);
	    		}
	    		s /= buffer.getChannels();
	    		f += s * s ;
	    		count++;
	    	}
    	}
    	if (count > 0) {
    		f = Math.sqrt(f / count) ;
    		f = Sample.toDB((float) f) ;
    	}
    	return f ;
    }
    
    boolean CheckNextSlices(SampleBuffer buffer, double prevsliceDB) {
    	boolean bret = true;
    	
    	double thresholdDB = Sample.toDB(mThresholdLinear) ;
    	

    	float[] a_sample = new float[buffer.getChannels()] ;
    	float f = 0.0f ;
    	
    	
    	//System.out.printf("thresholdDB = %f ; prevdb = %f\n", thresholdDB, prevsliceDB);
    	
    	long totalsamples = getMinDetectRequireSlice()*buffer.CalculateTimeToSamples(getSliceTime());
    	long step = 0;
    	long aslicesamples = buffer.CalculateTimeToSamples(getSliceTime());
    	
    	double rms[] = new double[getMinDetectRequireSlice()] ;
    	
    	for (int i = 0; i < getMinDetectRequireSlice(); i++) {
    		step = 0;
    		rms[i] = 0.0 ;
    		while(step < aslicesamples) {
    			buffer.get(a_sample);
    			f = 0.0f;
    			for (float s : a_sample) {
    				f += s ;
    			}
    			f = f / a_sample.length ;
    			rms[i] += f * f;
    			
    			step++ ;
    		}
    		
    		rms[i] = Math.sqrt(rms[i]/aslicesamples) ;
    		rms[i] = Sample.toDB((float)rms[i]) ;
    		//System.out.printf("rms[%d] = %f\n",i,rms[i]);
    		if (thresholdDB > rms[0]) {
    			//System.out.println("break detect.") ;
    			return false ;
    		}
    	}
    	
    	if (/*(thresholdDB <= rms[0] || Math.abs(thresholdDB - rms[0]) < 1.0) &&*/ 
    			Math.abs(rms[0] - prevsliceDB)>5.0) {
	    	for (int i = 1; i < getMinDetectRequireSlice(); i++) {
	    		if (rms[i] > (rms[i-1]+2.0) ) {
	    			
	    			bret = false;
	    			
	    			if (rms[i] >= thresholdDB || Math.abs(thresholdDB)-rms[i] < 1.0 
	    					&& Math.abs(rms[i]-prevsliceDB) > 5.0) {
	    				buffer.position(buffer.position() - (int)((getMinDetectRequireSlice()-i)*(buffer.CalculateTimeToSamples(getSliceTime()))));
	    				//System.out.printf("Next from slice[%d]", i);
	    				break ;
	    			}
	    		}else if (rms[1] < -20.0 || rms[2] < -20.0 || (rms[1] - rms[0]) < - 15.0) {
	    			bret = false ;
	    			//System.out.println("drop too fast! don't think it's gun shot.");
	    			break ;
	    		}
	    			
	    	}
    	}
    	else
    		bret = false;
    	
    	//System.out.println(bret) ;
    	//System.out.printf("\n");
    	
    	return bret ;
    }
    /**
     * @return the minimized time(ms) of this Algorithms ;
     */
    @Override
    public int getMinDetectRequireSlice() {
        return 5 ;
    }

    /**
     * @return the slice time(ms) of this Algorithms.
     */
    @Override
    public int getSliceTime() {
        return 20;
    }

    public AmplitudeShotDetectAlgorithms() {
    	mThresholdLinear = 1.0f;
    }

    public AmplitudeShotDetectAlgorithms(float ThresholdDB) {
    	
    	mThresholdLinear = Sample.DBtoLinear(ThresholdDB) ;
    }
}
