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


package com.bangz.audio;



import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.AudioFormat.ENCODING_PCM_8BIT;

/**
 * Audio Data: keep audio samples as float format ,range is (-1.0,1.0)
 * @author Royer Wang
 *
 */
public class SampleBuffer  {

	private int mChannels ;
    private int mSampleRate ;
	private FloatBuffer mSampleBuffer ;


	//public static final int ENCODING_PCM_16BIT = 2;
	//public static final int ENCODING_PCM_8BIT = 1 ;
	
    /**
     *
     * @param samplerate
     * @param channels
     * @param samples the buffer can hold how many samples. one sample include all channel data. so the buffer capacity
     *                is samples * channels.
     */
    public SampleBuffer(int samplerate, int channels, int samples) {

        mSampleRate = samplerate ;
        mChannels = channels ;

        mSampleBuffer = FloatBuffer.allocate(samples*channels);
    }

    public SampleBuffer(int samplerate, int channels, final ByteBuffer bytes, int audioformat) {
        mSampleRate = samplerate ;
        mChannels = channels ;

        Initialize(samplerate,channels,bytes,audioformat);
    }

    public SampleBuffer(int samplerate, int channels, FloatBuffer buffer) {

        if(buffer.capacity() % channels != 0 ) {
            throw new IllegalArgumentException("the FloatBuffer capacity must be multiple of channels.");
        }

        mSampleRate = samplerate ;
        mChannels = channels ;
        mSampleBuffer = buffer ;
    }

    public SampleBuffer(int samplerate, int channels, float[] array) {

        if (array.length % channels != 0 ) {
            throw new IllegalArgumentException("the float array length must be muitipe of channles.");
        }
        mSampleRate = samplerate ;
        mChannels = channels ;

        mSampleBuffer = FloatBuffer.wrap(array) ;
    }

    public SampleBuffer(int samplerate, int channles, float[] array, int start, int floatcount) {
        if (floatcount % channles != 0) {
            throw new IllegalArgumentException("floatcount must be multiple of channels.") ;
        }

        mSampleRate = samplerate ;
        mChannels = channles ;

        mSampleBuffer = FloatBuffer.wrap(array, start, floatcount);
    }

    private SampleBuffer() {}

    private void Initialize(int samplerate, int channels, final ByteBuffer bytes, int audioformat) {





        if (audioformat != ENCODING_PCM_8BIT && audioformat != ENCODING_PCM_16BIT) {
            throw new IllegalArgumentException("Only support 16bit PCM or 8bit PCM format.");
        }

        int capacity = bytes.limit() / ((audioformat == ENCODING_PCM_8BIT)?1:2) ;

        if(capacity % mChannels != 0) {
            throw new IllegalArgumentException("the bytes length must be multipe of channels");
        }

        mSampleBuffer = FloatBuffer.allocate(capacity);

        bytes.rewind();


        if (audioformat == ENCODING_PCM_16BIT)
            bytes.order(ByteOrder.LITTLE_ENDIAN) ;

        while(bytes.hasRemaining()) {
            float f ;
            if (audioformat == ENCODING_PCM_8BIT) {


                f = (float)bytes.get()/ Byte.MAX_VALUE ;
            }
            else {
                f = (float)bytes.getShort() / Short.MAX_VALUE ;
            }

            //clip
            if (f > 1.0f)
                f = 1.0f ;
            else if (f < -1.0f)
                f = -1.0f ;

            mSampleBuffer.put(f) ;
        }

        mSampleBuffer.rewind();
    }

    public int getChannels() {
        return mChannels ;
    }

    public int getSampleRate() {
        return mSampleRate ;
    }

    /** just wrap {@link java.nio.FloatBuffer#capacity()}*/
    public final int capacity() {

        return mSampleBuffer.capacity();
    }

    public static final long TimeToSample(int samplerate, int millisecond) {
    	return (long)((float)millisecond * samplerate / 1000 + 0.5);
    }
    public final long CalculateTimeToSamples(int millisecond) {
    	return TimeToSample(mSampleRate, millisecond);
    }

    /**
     * Get how many samples in this buffer. one sample is include all channels.
     * @return how many samples in the buffer.
     */
    public final int samples() {
        return mSampleBuffer.capacity() / mChannels ;
    }

    /** get a sample. return float[].length = mChannels. and the position increases mChannels */
    public float[] getsample() {

        float[] sample = new float[mChannels] ;
        get(sample);

        return sample ;
    }

    /** get current position of millisecond . */
    public float getTimePosition() {
        int position = mSampleBuffer.position() ;

        return (float)position * 1000.0f / mChannels / mSampleRate ;
    }

    /**
     *  the array that backs the samples buffer. just simple wrap {@link java.nio.FloatBuffer#array()}
     * @return the array ;
     */
    public final float[] array() {
        return mSampleBuffer.array() ;
    }

    /** just wrap {@link java.nio.FloatBuffer#arrayOffset()}    */
    public final int arrayOffset() {
        return mSampleBuffer.arrayOffset();
    }

    /** just wrap {@link java.nio.FloatBuffer#clear()} */
    public final SampleBuffer clear() {

        mSampleBuffer.clear();

        return this ;
    }

    /** just wrap {@link java.nio.FloatBuffer#flip()} */
    public final SampleBuffer flip() {

        mSampleBuffer.flip();

        return this ;
    }

    /** just wrap {@link java.nio.FloatBuffer#hasArray()}*/
    public final boolean hasArray() {
        return mSampleBuffer.hasArray() ;
    }

    /** just wrap {@link java.nio.FloatBuffer#hasRemaining()}*/
    public final boolean hasRemaining() {
        return mSampleBuffer.hasRemaining();
    }

    /** just wrap {@link java.nio.FloatBuffer#isReadOnly()}*/
    public boolean isReadOnly() {
        return mSampleBuffer.isReadOnly();
    }

    /** just wrap {@link java.nio.FloatBuffer#limit()}*/
    public final int limit() {
        return mSampleBuffer.limit() ;
    }

    /** just wrap {@link java.nio.FloatBuffer#limit(int)}*/
    public final SampleBuffer limit(int newLimit) {

        mSampleBuffer.limit(newLimit) ;

        return this ;
    }

    /** just wrap {@link java.nio.FloatBuffer#mark()}*/
    public final SampleBuffer mark() {
        mSampleBuffer.mark() ;
        return this ;
    }

    /** just wrap {@link java.nio.FloatBuffer#position(int)}*/
    public final SampleBuffer position(int newPostion) {
        mSampleBuffer.position(newPostion) ;
        return this ;
    }

    /** just wrap {@link java.nio.FloatBuffer#position()}*/
    public final int position() {
        return mSampleBuffer.position() ;
    }
    
    /** get current sample position */
    public final int position_of_sample() {
    	return mSampleBuffer.position() / mChannels ;
    }

    /** just wrap {@link java.nio.FloatBuffer#remaining()}*/
    public final int remaining() {
        return mSampleBuffer.remaining();
    }

    /** get remaining samples */
    public final int remainingsamples() {
        return mSampleBuffer.remaining() / mChannels ;
    }

    /** just wrap {@link java.nio.FloatBuffer#reset()}*/
    public final SampleBuffer reset() {
        mSampleBuffer.reset() ;
        return this ;
    }

    /** just wrap {@link java.nio.FloatBuffer#rewind()} */
    public final SampleBuffer rewind() {
        mSampleBuffer.rewind() ;
        return this ;
    }

    /** just wrap {@link java.nio.FloatBuffer#compact() } */
    public SampleBuffer compat() {
        mSampleBuffer.compact() ;
        return this ;
    }

    /** just wrap {@link java.nio.FloatBuffer#compareTo(java.nio.FloatBuffer)}*/
    public int compareTo(SampleBuffer other) {
        return mSampleBuffer.compareTo(other.mSampleBuffer) ;
    }

    public SampleBuffer duplicate() {

        SampleBuffer tnew = new SampleBuffer() ;

        tnew.mSampleRate = this.mSampleRate ;
        tnew.mChannels = this.mChannels ;
        tnew.mSampleBuffer = this.mSampleBuffer.duplicate() ;

        return tnew ;
    }


    /** just wrap {@link java.nio.FloatBuffer#get(int index) } */
    public float get(int index) {
        return mSampleBuffer.get(index);
    }

    /** just wrap {@link java.nio.FloatBuffer#get()} */
    public float get() {
        return mSampleBuffer.get();
    }

    /** just wrap {@link java.nio.FloatBuffer#get(float[])} */
    public FloatBuffer get(float[] dst) {
        return mSampleBuffer.get(dst) ;
    }

    /** just wrap {@link java.nio.FloatBuffer#get(float[], int, int)}*/
    public FloatBuffer get(float[] dst, int dstOffset, int floatCount) {
        return mSampleBuffer.get(dst, dstOffset, floatCount) ;
    }


    /** just wrap {@link java.nio.FloatBuffer#put(int, float)}*/
    public SampleBuffer put(int index, float f) {
        mSampleBuffer.put(index, f) ;
        return this ;
    }

    /** just wrap {@link java.nio.FloatBuffer#put(float[], int, int)}*/
    public SampleBuffer put(float[] src, int srcOffset, int floatCount) {
        mSampleBuffer.put(src, srcOffset, floatCount);
        return this ;
    }

    /** proxy {@link java.nio.FloatBuffer#put(java.nio.FloatBuffer)} , and both src and dest SampleBuffer must have same
     *  mSamplerate and mChannels
     */
    public SampleBuffer put(SampleBuffer src) {

        if (src.mSampleRate != this.mSampleRate || src.mChannels != this.mChannels) {
            throw new IllegalArgumentException("src sample rate or channels is not same as dest.");
        }
        mSampleBuffer.put(src.mSampleBuffer) ;
        return this ;
    }

    /** just wrap {@link java.nio.FloatBuffer#put(float[])}*/
    public final SampleBuffer put(float[] src) {
        mSampleBuffer.put(src) ;
        return this ;
    }

    /** just wrap {@link java.nio.FloatBuffer#put(float)}*/
    public SampleBuffer put(float f) {
        mSampleBuffer.put(f) ;
        return this ;
    }


    public static SampleBuffer wrap(int samplerate, int channels, float[] array) {
        return new SampleBuffer(samplerate, channels, array) ;
    }

    public static SampleBuffer wrap(int samplerate,int channels, float[] array, int start, int floatcount) {
        return new SampleBuffer(samplerate, channels, array, start, floatcount) ;
    }
}
