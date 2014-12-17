package video;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.xuggle.mediatool.IMediaDebugListener.Event;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaTool;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.MediaToolAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IAudioSamplesEvent;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;

public class XuggleTest {

    private static final String filename = "D:/worksap/HUE-kickoff/team-introduction/SRE.MTS";
    private static final String targetFilename = "D:/worksap/HUE-kickoff/team-introduction/SRE.mp4";
    private static final String targetAvi = "D:/worksap/HUE-kickoff/team-introduction/SRE.avi";
    private static final String targetImage = "D:/worksap/HUE-kickoff/team-introduction/worksLogo.png";

    @Test
    public void testBasicInfo() {
        // first we create a Xuggler container object
        IContainer container = IContainer.make();

        // we attempt to open up the container
        int result = container.open(filename, IContainer.Type.READ, null);

        // check if the operation was successful
        if (result < 0)
            throw new RuntimeException("Failed to open media file");

        // query how many streams the call to open found
        int numStreams = container.getNumStreams();

        // query for the total duration
        long duration = container.getDuration();

        // query for the file size
        long fileSize = container.getFileSize();

        // query for the bit rate
        long bitRate = container.getBitRate();

        System.out.println("Number of streams: " + numStreams);
        System.out.println("Duration (ms): " + duration);
        System.out.println("File Size (bytes): " + fileSize);
        System.out.println("Bit Rate: " + bitRate);

        // iterate through the streams to print their meta data
        for (int i = 0; i < numStreams; i++) {

            // find the stream object
            IStream stream = container.getStream(i);

            // get the pre-configured decoder that can decode this stream;
            IStreamCoder coder = stream.getStreamCoder();

            System.out.println("*** Start of Stream Info ***");

            System.out.printf("stream %d: ", i);
            System.out.printf("type: %s; ", coder.getCodecType());
            System.out.printf("codec: %s; ", coder.getCodecID());
            System.out.printf("duration: %s; ", stream.getDuration());
            System.out.printf("start time: %s; ", container.getStartTime());
            System.out.printf("timebase: %d/%d; ", stream.getTimeBase()
                    .getNumerator(), stream.getTimeBase().getDenominator());
            System.out.printf("coder tb: %d/%d; ", coder.getTimeBase()
                    .getNumerator(), coder.getTimeBase().getDenominator());
            System.out.println();

            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                System.out.printf("sample rate: %d; ", coder.getSampleRate());
                System.out.printf("channels: %d; ", coder.getChannels());
                System.out.printf("format: %s", coder.getSampleFormat());
            } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                System.out.printf("width: %d; ", coder.getWidth());
                System.out.printf("height: %d; ", coder.getHeight());
                System.out.printf("format: %s; ", coder.getPixelType());
                System.out.printf("frame-rate: %5.2f; ", coder.getFrameRate()
                        .getDouble());
            }

            System.out.println();
            System.out.println("*** End of Stream Info ***");

        }

    }

    @Test
    public void testTranscoding() {
        IMediaReader mediaReader = ToolFactory.makeReader(filename);
        IMediaWriter mediaWriter = ToolFactory.makeWriter(targetFilename,
                mediaReader);

        mediaReader.addListener(mediaWriter);

        // create a media viewer with stats enabled
        //        IMediaViewer mediaViewer = ToolFactory.makeViewer(true);
        //        
        //        mediaReader.addListener(mediaViewer);

        while (mediaReader.readPacket() == null)
            ;
    }

    // reader -> addStaticImage -> reduceVolume -> writer 
    @Test
    public void testModifyMedia() {
        IMediaReader mediaReader = ToolFactory.makeReader(targetFilename);

        // configure it to generate buffer images
        mediaReader
                .setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);

        IMediaWriter mediaWriter = ToolFactory.makeWriter(targetAvi,
                mediaReader);

        IMediaTool imageMediaTool = new StaticImageMediaTool(targetImage);
        IMediaTool audioVolumeMediaTool = new VolumeAdjustMediaTool(0.3);

        mediaReader.addListener(imageMediaTool);
        imageMediaTool.addListener(audioVolumeMediaTool);
        audioVolumeMediaTool.addListener(mediaWriter);

        while (mediaReader.readPacket() == null)
            ;
    }

    private static class StaticImageMediaTool extends MediaToolAdapter {

        private BufferedImage logoImage;

        public StaticImageMediaTool(String imageFile) {
            try {
                logoImage = ImageIO.read(new File(imageFile));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("could not open file");
            }
        }

        @Override
        public void onVideoPicture(IVideoPictureEvent event) {
            BufferedImage image = event.getImage();

            long ts = event.getTimeStamp();
            if (ts / 1000000 > 15) {
                if (ts / 1000000 % 2 == 1) {
                    Graphics2D g = image.createGraphics();
                    Rectangle2D bounds = new Rectangle2D.Float(0, 0,
                            logoImage.getWidth(), logoImage.getHeight());

                    // compute the amount to inset the time stamp and translate the image to that position
                    double insetX = bounds.getWidth();
                    double insetY = bounds.getHeight();
                    //            g.translate(inset, event.getImage().getHeight() - inset);
                    g.translate(insetX, insetY);

                    g.setColor(Color.WHITE);
                    g.fill(bounds);
                    g.setColor(Color.BLACK);
                    g.drawImage(logoImage, 0, 0, null);
                }
                // call parent which will pass the video to next tool in chain
                super.onVideoPicture(event);
            }
        }
    }

    private static class VolumeAdjustMediaTool extends MediaToolAdapter {

        private double mVolume;

        public VolumeAdjustMediaTool(double volume) {
            mVolume = volume;
        }

        @Override
        public void onAudioSamples(IAudioSamplesEvent event) {
            long ts = event.getTimeStamp();
            if (ts / 1000000 > 15) {
                ShortBuffer buffer = event.getAudioSamples().getByteBuffer()
                        .asShortBuffer();

                for (int i = 0; i < buffer.limit(); ++i) {
                    buffer.put(i, (short) (buffer.get(i) * mVolume));
                }

                super.onAudioSamples(event);
            }
        }

    }

    @Test
    public void testSplittingIntoTwo() {
        String source = "D:/worksap/HUE-kickoff/team-introduction/SRE.mp4";
        String target1 = "D:/worksap/HUE-kickoff/team-introduction/SRE-1.mp4";
        String target2 = "D:/worksap/HUE-kickoff/team-introduction/SRE-2.mp4";

        IMediaReader reader = ToolFactory.makeReader(source);
        CutChecker cutChecker = new CutChecker();
        reader.addListener(cutChecker);
        IMediaWriter writer = ToolFactory.makeWriter(target1, reader);
        cutChecker.addListener(writer);

        boolean updated = false;
        while (reader.readPacket() == null) {
            // 15 below is the point to split, in seconds
            if ((cutChecker.currentTimestamp >= 15 * 1000000) && (!updated)) {
                cutChecker.removeListener(writer);
                writer.close();
                writer = ToolFactory.makeWriter(target2, reader);
                cutChecker.addListener(writer);
                updated = true;
            }
        }
    }

    private static class CutChecker extends MediaToolAdapter {

        public long currentTimestamp = 0L;

        @Override
        public void onVideoPicture(IVideoPictureEvent event) {
            currentTimestamp = event.getTimeStamp();
            super.onVideoPicture(event);
        }

    }

    @Test
    public void testSplittingOnlyOne() {
        String source = "D:/worksap/HUE-kickoff/team-introduction/SRE.mp4";
        String target3 = "D:/worksap/HUE-kickoff/team-introduction/SRE-3.mp4";

        IMediaReader reader = ToolFactory.makeReader(source);
        CutChecker cutChecker = new CutChecker();
        reader.addListener(cutChecker);

        boolean updated = false;
        while (reader.readPacket() == null) {
            // 15 below is the point to split, in seconds
            if ((cutChecker.currentTimestamp >= 15 * 1000000) && (!updated)) {
                IMediaWriter writer = ToolFactory.makeWriter(target3, reader);
                cutChecker.addListener(writer);
                updated = true;
            }
        }
    }

}
