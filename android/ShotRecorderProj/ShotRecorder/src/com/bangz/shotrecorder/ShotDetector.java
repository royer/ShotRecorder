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

import android.media.AudioFormat;

import com.bangz.audio.SampleBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;


/**
 * the class for shot detection .
 * ShotDetector process the whole detect, return the shot event happened time in millisecond.
 *
 * Keep Shot Detector instance in whole time.
 * call start to begin/restart a detect process, and next each doDetect call
 * is continue this detect process.
 * <b>NOTE: the whole detecting is not thread safe</b>.
 *
 * <p></p>
 * the doDetect return value long[] is each shot event happen time (millisecond)
 * relative the whole detect process that after call start method.
 *
 * <p></p>
 * The interface {@link com.bangz.shotrecorder.ShotDetector.ShotDetectAlgorithms#DetectShotAlgorithms(com.bangz.audio.SampleBuffer, com.bangz.audio.SampleBuffer)}
 * return value long[] is <b>sample offset</b> of the argument buffer, <b>IT IS NOT the original position
 * of the FloatBuffer.</b>
 */
public class ShotDetector {

    /** the default audio recognize slice time .usually is 20 millisecond. */
    public static int DEFAULT_SLICE_TIME = 20 ;
    
    

    public interface ShotDetectAlgorithms {
        /**
         * the algorithms of detect the shot event. the implement must through all samples.
         *
         * @param buffer : the samples to detect. it round with sample, it mean if this is a two channels (STEREO)
         *               the buffsize size must be power of 2. if you need more samples for do detect, you should
         *               set the correct position value. ShotDetector flip remaining sample to the buffer beging,
         *               and fill next samples when they ready. 
         *               
         * @return  each element of long[] is sample offset of the buffer. it length may be zero to indicate no
         *          shot event be detected.
         */
        public Long[] DetectShotAlgorithms(SampleBuffer buffer, SampleBuffer prevSlice);

        /**
         *
         * @return the minimized time(ms) of this Algorithms ;
         */
        public int getMinDetectRequireSlice() ;

        /**
         *
         * @return the slice time(ms) of this Algorithms.
         */
        public int getSliceTime() ;
    }


    private ShotDetectAlgorithms    m_Detector = null ;

    private int mSampleRate ;
    private int mChannels ;

    /** total processed samples. */
    private long mProcessedSamples;

    /**
     * previous slice. may be some algorithms need this. If its limit is 0 ,it mean there has not prev slice.
     *
     */
    private SampleBuffer m_PrevSlice;

    /*
     * the actually process buffer, it's capacity is mMaxProcessSamples + ShotDetectAlgorithms#getMinBufferTime
     * + ShotDetectAlgorithms#getSliceTime();
     */
    private SampleBuffer m_Buffer ;
    
    
    /**
     * when call doDetector method, the @param buffer max samples ;
     */
    private int mMaxProcessSamples ;



    public ShotDetector() {

    }

    public ShotDetector(int samplerate, int channels, ShotDetectAlgorithms algorithms, int maxprocesssamples ) {
        initialize(samplerate, channels, algorithms, maxprocesssamples);
    }

    private void initialize(int samplerate, int channels, ShotDetectAlgorithms algorithms, int maxprocesssamples) {
        mSampleRate = samplerate ;
        mChannels = channels ;

        setDetectAlgorithms(algorithms) ;


    }


    public SampleBuffer getSampleBuffer() {

        return m_Buffer ;
    }

    /**
     * Internal detect. m_Buffer is ready
     * @return
     */
    private Long[] doDetect() {
    	
    	if (m_Detector == null) 
    		return new Long[0] ;
    	
    	Long[] ll = m_Detector.DetectShotAlgorithms(m_Buffer, m_PrevSlice);
    	
    	for(int i = 0; i < ll.length; i++) {
    		ll[i] = Math.round((ll[i] + this.mProcessedSamples) * 1000.0 / mSampleRate) ;
    	}
    	
    	mProcessedSamples += m_Buffer.position_of_sample() ;

    	// make the m_PrevSlice
		int oldposition = m_Buffer.position();
		
		int need_cp_to_prevslice = Math.min(m_PrevSlice.capacity(), m_Buffer.position()) ;
		
		ShiftLeftPrevSlice(need_cp_to_prevslice) ;
		
		m_PrevSlice.limit(Math.min(m_PrevSlice.capacity(), m_PrevSlice.limit() + need_cp_to_prevslice)) ;
		m_PrevSlice.position(m_PrevSlice.limit() - need_cp_to_prevslice) ;

		m_Buffer.position(m_Buffer.position() - need_cp_to_prevslice) ;
		
		while(m_PrevSlice.hasRemaining()) {
			m_PrevSlice.put(m_Buffer.get());
		}
		
		m_Buffer.rewind();
		//shift m_Buffer for next read
		for(int src = oldposition; src < m_Buffer.limit(); src++) {
			m_Buffer.put(m_Buffer.get(src));
		}
    	
    	return ll ;
    }
    
    private void ShiftLeftPrevSlice(int needspace) {
    	int srcstart = needspace ;
    	for (int src = srcstart, dest = 0; src < m_PrevSlice.limit(); src++, dest++) {
    		m_PrevSlice.put(dest,m_PrevSlice.get(src));
    	}
    	
    }
    

    public Long[] doDetect(final float[] buffer, int sampleRate, int channels) {
    	
    	if (m_Detector == null)
    		return new Long[0] ;
    	
    	if (sampleRate != mSampleRate || mChannels != channels) {
    		throw new IllegalArgumentException("the sampleRate and channles must same as Detector setting.") ;
    	}
    	
    	ArrayList<Long> lresult = new ArrayList<Long>();
    	
    	int i = 0; ;
    	
    	while(i != buffer.length ) {
    		
    		while(i!=buffer.length && m_Buffer.hasRemaining()) {
    			m_Buffer.put(buffer[i++]) ;
    		}
    		
    		m_Buffer.limit(m_Buffer.position()) ;
    		m_Buffer.rewind() ;
    		
    		Long[] ll = doDetect();
    		
    		for (Long l : ll) {
    			lresult.add(l);
    		}
    		
    		m_Buffer.limit(m_Buffer.capacity()) ;
    	}
    	
    	return lresult.toArray(new Long[lresult.size()]) ;
    }
    
    public Long[] doDetect(final byte[] buffer, int sampleRate, int channels, int audioformat) {
    	
    	ByteBuffer bb = ByteBuffer.wrap(buffer) ;
    	
    	return doDetect(bb, sampleRate, channels, audioformat) ;
    }
    
    public Long[] doDetect(final short[] buffer, int sampleRate, int channels) {
    	
    	ShortBuffer sb = ShortBuffer.wrap(buffer) ;
    	
    	return doDetect(sb, sampleRate, channels) ;
    }
    
    public Long[] doDetect(final ShortBuffer buffer, int sampleRate, int channels) {
    	
    	if (m_Detector == null)
    		return new Long[0] ;

        if (sampleRate != mSampleRate || mChannels != channels) {
            throw new IllegalArgumentException("the sampleRate and channles must same as Detector setting.") ;
        }

    	ArrayList<Long> lresult = new ArrayList<Long>();
    	
    	while(buffer.hasRemaining()) {
    		
    		while(buffer.hasRemaining() && m_Buffer.hasRemaining()) {
    			float f = buffer.get() * 1.0f / Short.MAX_VALUE;
    			if (f > 1.0f) 
    				f = 1.0f;
    			else if (f < -1.0)
    				f = - 1.0f;
    			m_Buffer.put(f);
    		}
    		
    		m_Buffer.limit(m_Buffer.position());
    		m_Buffer.rewind();
    		
    		Long[] ll = doDetect();
    		
    		for (Long l : ll)
    			lresult.add(l);
    		
    		m_Buffer.limit(m_Buffer.capacity());
    	}
    	
    	return lresult.toArray(new Long[lresult.size()]) ;
    }
    
    public Long[] doDetect(final ByteBuffer buffer, int sampleRate, int channels, int audioformat) {

        if (m_Detector == null)
            return new Long[0];

        if (sampleRate != mSampleRate || mChannels != channels) {
            throw new IllegalArgumentException("the sampleRate and channles must same as Detector setting.") ;
        }


        if (audioformat != AudioFormat.ENCODING_PCM_8BIT && AudioFormat.ENCODING_PCM_16BIT != audioformat) {
            throw new IllegalArgumentException("only support PCM_8BIT or PCM_16BIT");
        }


        if (audioformat == AudioFormat.ENCODING_PCM_16BIT)
            buffer.order(ByteOrder.LITTLE_ENDIAN);

        ArrayList<Long> lresult = new ArrayList<Long>();


        while(buffer.hasRemaining()) {

            while(buffer.hasRemaining() && m_Buffer.hasRemaining() ) {

                float f ;
                if(audioformat == AudioFormat.ENCODING_PCM_16BIT)
                    f = (float)buffer.getShort() / Short.MAX_VALUE ;
                else
                    f = (float)buffer.get() / Byte.MAX_VALUE ;

                if (f > 1.0)
                	f = 1.0f;
                else if (f < -1.0)
                	f = - 1.0f ;
                
                m_Buffer.put(f) ;
            }
            
            m_Buffer.limit(m_Buffer.position()) ;
            m_Buffer.rewind() ;
            
            Long[] ll = doDetect() ;

            for(Long l : ll) {
            	lresult.add(l) ;
            }
            

            m_Buffer.limit(m_Buffer.capacity());
            
        }

        return lresult.toArray(new Long[lresult.size()]) ;

    }


    public void start() {

        if (mSampleRate == 0 || mChannels == 0 || m_Detector == null) {
            throw new IllegalArgumentException("call start. but either mSampleRate or mChannels is 0. ");
        }
        mProcessedSamples = 0 ;


        if (m_Buffer != null)
            m_Buffer.clear();

        if(m_PrevSlice != null) {
            m_PrevSlice.clear();
            m_PrevSlice.limit(0) ;
        }
    }


    public ShotDetectAlgorithms setDetectAlgorithms(ShotDetectAlgorithms algorithms) {

        ShotDetectAlgorithms old = m_Detector ;

        m_Detector = algorithms ;

        if (m_Detector != null) {

            m_Buffer = new SampleBuffer(mSampleRate, mChannels, 
            		(int)(SampleBuffer.TimeToSample(mSampleRate, m_Detector.getSliceTime()) * m_Detector.getMinDetectRequireSlice()) + mMaxProcessSamples) ;

            m_PrevSlice = new SampleBuffer(mSampleRate, mChannels, 
            		(int)SampleBuffer.TimeToSample(mSampleRate, m_Detector.getSliceTime())) ;
            
            m_PrevSlice.limit(0);
        } else {
        	m_Buffer = null ;
        	m_PrevSlice = null ;
        }

        return old;
    }

    public ShotDetectAlgorithms getDetectAlgorithms() {

        return m_Detector ;
        
    }

}
