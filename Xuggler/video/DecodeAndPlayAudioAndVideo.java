package video;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;

/**
 * Using {@link IMediaReader}, takes a media container, finds the first video stream,
 * decodes that stream, and then plays the audio and video.
 *
 * @author aclarke
 * @author trebor
 */

public class DecodeAndPlayAudioAndVideo extends MediaListenerAdapter {
    /**
     * Takes a media container (file) as the first argument, opens it,
     * plays audio as quickly as it can, and opens up a Swing window and
     * displays video frames with <i>roughly</i> the right timing.
     *  
     * @param args Must contain one string which represents a filename
     */

    public static void main(String[] args) {
        args = new String[] { "D:\\worksap\\HUE-kickoff\\team-introduction\\SRE.mp4" };
        if (args.length <= 0)
            throw new IllegalArgumentException(
                    "must pass in a filename as the first argument");

        // create a new mr. decode an play audio and video
        IMediaReader reader = ToolFactory.makeReader(args[0]);
        reader.addListener(ToolFactory.makeViewer());
        while (reader.readPacket() == null)
            do {
            } while (false);

    }

}