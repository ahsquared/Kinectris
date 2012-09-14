package kinectris32;
import processing.core.*;
import rwmidi.*;

public class MidiTimer extends PApplet implements Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8939567253634506378L;
	
	MidiOutput output;
	boolean isRunning = true;
	boolean isActive = false;
	int noteLength;
	int pitch;
	int channel;
	
	MidiTimer(int c, int device) {
		isRunning = true;
		channel = c;
		pitch = 64;
		noteLength = 250;
		output = RWMidi.getOutputDevices()[device].createOutput();
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		try {
			while (isRunning) {
				while (isActive) {
					Thread.sleep(noteLength);
					output.sendNoteOff(channel, pitch, 0);
					//println("turned note "  pitch  " off");
					isActive = false;
				}
				Thread.sleep(25);
			}
		}
		catch(InterruptedException e) {
		}
	} 

	void setNote(int c, int p, int l) {
		channel = c;
		pitch = p;
		noteLength = l;
		isActive = true;
	}
	
	void quit() {
		isRunning = false;  // Setting running to false ends the loop in run()
	}
}
