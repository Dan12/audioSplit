import com.sun.media.sound.WaveFileWriter;
import com.sun.org.apache.regexp.internal.RE;
import it.sauronsoftware.jave.*;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Created by Danweb on 5/29/16.
 */
public class OldMain {

    public static int filenum = 0;
    public static int emptyTol = 8;
    public static int emptyLen = 20;
    public static int moveBufferHead = -1000;
    public static int moveBufferTail = -5000;
    public static int roundBuffer = 128;
    public static boolean deleteWav = false;
    public static boolean createMP3 = true;

    public static void main(String[] args) {
//        String fileName="2select.mp3";
//        String fileName="chain1A.mp3";
        String fileName = "chain1.wav";
        File file = Paths.get(".", "res", fileName).normalize().toFile();
        System.out.println(Paths.get(".", "res", fileName).normalize());
        System.out.println(file.exists());
        AudioInputStream in= null;
        try {
            in = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        AudioInputStream inLength= null;
        try {
            inLength = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        AudioInputStream din = null;
        AudioFormat baseFormat = in.getFormat();
        AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false);
        din = AudioSystem.getAudioInputStream(decodedFormat, in);
        AudioInputStream dinLength = AudioSystem.getAudioInputStream(decodedFormat, inLength);
        int numBytesRead = 0;
        byte[] audioBytes = new byte[4096];
        int totalRead = 0;

        try {
            while ((numBytesRead = dinLength.read(audioBytes)) != -1) {
                totalRead+=numBytesRead;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //System.out.println(totalRead);
        totalRead = (totalRead/4096)*4096;
        byte[] allBytes = new byte[totalRead];
        int b = 0;

        try {
            while ((numBytesRead = din.read(audioBytes)) != -1) {
                //totalRead+=numBytesRead;
                //System.out.println(numBytesRead);
                //System.out.println(Arrays.toString(audioBytes));
                if(b < totalRead){
                    System.arraycopy(audioBytes,0,allBytes,b,4096);
                    b+=4096;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(Arrays.toString(allBytes));


        for(int i = 0; i < allBytes.length; i++){
            if(Math.abs(allBytes[i]) > emptyTol){
                int startPos = i;
                boolean isValid = true;
                for(i = i; i < startPos+emptyLen; i++){
                    if(Math.abs(allBytes[i]) <= emptyTol){
                        isValid = false;
                        break;
                    }
                }
                if(isValid){
                    while(i < allBytes.length){
                        i++;
                        if(Math.abs(allBytes[i]) <= emptyTol){
                            boolean isOver = true;
                            int endPos = i;
                            for(i = i; i < endPos+emptyLen; i++){
                                if(Math.abs(allBytes[i]) > emptyTol){
                                    isOver = false;
                                    break;
                                }
                            }
                            if(isOver){
                                newFile(allBytes, startPos, endPos);
                                break;
                            }
                        }
                    }
                }
            }
        }


        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void newFile(byte[] source, int start, int stop){
        if(filenum > 100){
            System.out.println("Too many files");
        }
        else {
            System.out.println("New File" + filenum);
            byte[] temp = new byte[stop - start];
            System.arraycopy(source, ((start+moveBufferHead)/roundBuffer)*roundBuffer, temp, 0, ((stop - start+moveBufferTail)/roundBuffer)*roundBuffer);

            try {
                writeWavFile(temp, "f" + filenum);
                filenum++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void writeWavFile(byte[] resultArray,String name) throws IOException {

        createWaveFile(name, resultArray);

        if(createMP3) {
            //convertToMP3(name);
            convertmp3Lame(name);
        }
    }

    public static void convertmp3Lame(String name){
        Process p;
        try {
            p = Runtime.getRuntime().exec("lame -v --nogap "+name+".wav");
            try {
                p.waitFor();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(p.getInputStream()));

                String line = "";
                while ((line = reader.readLine())!= null) {
                    System.out.println(line + "\n");
                }
            } catch (InterruptedException e) {e.printStackTrace();}
        } catch (IOException e) {e.printStackTrace();}
    }

    public static void convertToMP3(String name) throws IOException {
        File source = Paths.get(".", "", name+".wav").normalize().toFile();
        System.out.println(source.exists());
        File target = Paths.get(".", "", name+".mp3").normalize().toFile();
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libmp3lame");
        audio.setBitRate(new Integer(128000));
        audio.setChannels(new Integer(2));
        audio.setSamplingRate(new Integer(44100));
        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setFormat("mp3");
        attrs.setOffset(-0.05f);
        attrs.setAudioAttributes(audio);

        FFMPEGLocator test = new FFMPEGLocator() {
            @Override
            protected String getFFMPEGExecutablePath() {
                return "./libs/ffmpeg";
            }
        };
        EncoderProgressListener lstn = new EncoderProgressListener() {
            @Override
            public void sourceInfo(MultimediaInfo multimediaInfo) {

            }

            @Override
            public void progress(int i) {
                System.out.println("Progress: "+i);
            }

            @Override
            public void message(String s) {
                System.out.println("Message: "+s);
            }
        };

        Encoder encoder = new Encoder(test);
        try {
            System.out.println(Arrays.toString(encoder.getAudioDecoders()));
            System.out.println(Arrays.toString(encoder.getAudioEncoders()));
        } catch (EncoderException e) {
            e.printStackTrace();
        }
        try {
            encoder.encode(source, target, attrs, lstn);
        } catch (EncoderException e) {
            e.printStackTrace();
        }
        if(deleteWav)
            Files.delete(Paths.get(".", "", name+".wav").normalize());
    }

    public static void createWaveFile(String filename, byte[] data) throws IOException {
        // assumes 44,100 samples per second
        // use 16-bit audio, stereo, signed PCM, little Endian
        AudioFormat format = new AudioFormat(44100, 16, 2, false, false);
//        byte[] data = new byte[2 * samples.length];
//        for (int i = 0; i < samples.length; i++) {
//            int temp = (short) (samples[i] * MAX_16_BIT);
//            data[2*i + 0] = (byte) temp;
//            data[2*i + 1] = (byte) (temp >> 8);
//        }

        // now save the file
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        AudioInputStream ais = new AudioInputStream(bais, format, data.length/2);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filename+".wav"));
    }

    public static void createWaveFileWithHeader(String name, byte[] resultArray) throws IOException {
        File f = new File(name+".wav");
        FileOutputStream fos = new FileOutputStream(f);
        WriteWaveFileHeader(fos, 0 , resultArray.length, 44100, 2, 176400);
        fos.write(resultArray);
        fos.flush();
        fos.close();
    }

    private static void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];
        byte RECORDER_BPP = 16;
        totalAudioLen = totalDataLen-44;
        totalDataLen = totalDataLen-8;
        byteRate = (longSampleRate * RECORDER_BPP * channels)/8;
        System.out.println(byteRate);

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * RECORDER_BPP / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }
}
