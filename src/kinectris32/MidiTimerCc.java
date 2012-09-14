package kinectris32;
import processing.core.*;
import rwmidi.*;

public class MidiTimerCc extends PApplet implements Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8939567253634506379L;
	
	MidiOutput output;
	boolean isRunning = true;
	boolean isActive = false;
	int msgLength;
	int pitch;
	int cc;
	int value;
	int channel;
	
	MidiTimerCc(int c, int device) {
		isRunning = true;
		channel = c;
		pitch = 64;
		cc = 1;
		value = 0;
		msgLength = 250;
		output = RWMidi.getOutputDevices()[device].createOutput();
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		try {
			while (isRunning) {
				while (isActive) {
					Thread.sleep(msgLength);

					output.sendController(channel, cc, value);

					//println("turned note "  pitch  " off");
					isActive = false;
				}
				Thread.sleep(25);
			}
		}
		catch(InterruptedException e) {
		}
	} 


	
	void setController(int c, int _cc, int v, int l) {
		channel = c;
		cc = _cc;
		value = v;
		msgLength = l;
		isActive = true;
	} 
	
	void quit() {
		isRunning = false;  // Setting running to false ends the loop in run()
	}
}
