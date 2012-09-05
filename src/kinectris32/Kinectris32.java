package kinectris32;

import java.util.HashMap;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import peasy.PeasyCam;
import javax.media.opengl.*;
import processing.opengl.*;
import processing.core.*;
import shapes3d.Box;
import shapes3d.Shape3D;
import shapes3d.utils.*;
import shapes3d.animation.*;
import shapes3d.*;
import beads.*;
import objimp.*;
import toxi.geom.*;
import toxi.physics.*;
import toxi.physics.behaviors.*;
import toxi.physics.constraints.*;
import themidibus.*;
import rwmidi.*;

//import geomerative.*;

public class Kinectris32 extends PApplet {

	/*public static boolean fullscreen = false;

    static public void main(String args[]) {
        if (fullscreen) {
            PApplet.main(new String[] { "--present", "Kinectris"});
        }
    }
    */

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	// debug mode switch
    public static boolean debug = false;
    

    // Kinect
    //SimpleOpenNI kinect;
    boolean autoCalib = true;

    int colT = color(255, 100, 100);
    int colP = color(100, 255, 100);
    PolygonTarget polygonTarget;
    PolygonPlayer polygonPlayer;
    boolean wallComing = true;
    PFont myFont;
    int scorePosX = 100;
    boolean paused, hit;
    int pauseCounter = 0;
    Star [] stars = new Star[200];
    Gem[] gems;
    
    // game states
    boolean hitTest = false;
    
    // position of player on screen
    float yOffset = 0;
    float playerScaleFactor = 1.5f;
    
    // screen dimensions
    float rightEdge, leftEdge, topEdge, bottomEdge, totalWidth, totalHeight;

    // Levels
    int level = 1;
    
    // create 3d model
    ObjImpScene scene;

    // Terrain
    Terrain terrain;
    float terrainSize = 1000;
    float horizon = 400;
    
    // Kinect Joints
    KinectData kinectData;
  
    // oscP5
    OscP5 oscP5;
    NetAddress myRemoteLocation;
    
    // audio using Beads
    AudioContext ac;
    String sourceAudioFile;
    SamplePlayer sp;
    Gain g;
    Glide gainValue;
    
    PImage tex;
    
    PeasyCam camera;
    
    Box box, stage, stage2, boxTarget;
    Ellipsoid world, worldOuter;
    
    int stageColor = color(186, 0, 203);
    int lineColor = color(235, 221, 0);
//    RShape wall;
//    RStyle wallStyle;
    
    
    // Physics
    VerletPhysics physics;
    VisibleBoxConstraint worldFloor;
    MaxConstraint floor;
    VerletParticle ball;
    
    // Midi
    MidiBus midiBus;
    MidiInput input;
    MidiOutput output;
    MidiTimer midiTimer;
    
    public void setup() {
        size(1280, 720, OPENGL);
        rightEdge = width*5/4;
        leftEdge = -width/4;
        topEdge = -height/2;
        bottomEdge = height*1.5f;
        totalWidth = rightEdge - leftEdge;
        totalHeight = bottomEdge - topEdge;
        float fov = PI/2.5f;
        float cameraZ = (height/1.875f) / tan(PI*60.0f/360.0f);
        perspective(fov, width/height, cameraZ/10.0f, cameraZ*16f);
        //perspective(radians(45), width/height, 10f,150000f);
        frameRate(30);
        myFont = createFont("Arial", 32);
        textFont(myFont);
        fill(0);
        hint(ENABLE_NATIVE_FONTS);
        textMode(SCREEN);
        camera = new PeasyCam(this, width/2, height/2, 200, 1150);
        
        tex = loadImage("/src/data/KAMEN.jpg");

//        RG.init( this );
//        wall = RG.loadShape("/src/data/Toucan.svg");
//        wallStyle = new RStyle();
//        wallStyle.texture = tex;
//        wall.setStyle(wallStyle);
        
        /* setup Kinect */
        /*try {
        	kinect = new SimpleOpenNI(this);
        	// disable mirror
            kinect.setMirror(true);
            // enable depthMap generation
            if(kinect.enableDepth() == false)
            {
                println("Can't open the depthMap, maybe the camera is not connected!");
                exit();
                return;
            }
            // enable skeleton generation for all joints
            kinect.enableUser(SimpleOpenNI.SKEL_PROFILE_ALL);
        } catch (Exception e) {
        	JOptionPane.showMessageDialog(frame, "SimpleOpenNI failed.");
        }*/
        

        // start oscP5 listening for incoming messages at port 3000
        oscP5 = new OscP5(this, 3001);
        myRemoteLocation = new NetAddress("127.0.0.1", 3001);
        oscP5.plug(this, "allJoints", "/skeleton0");
//        oscP5.plug(this, "wristLeft", "/skeleton0/WristLeft");
//        oscP5.plug(this, "shoulderRight", "kinect/skeleton/0/shoulder_right");
//        oscP5.plug(this, "shoulderLeft", "kinect/skeleton/0/shoulder_left");
//        oscP5.plug(this, "footRight", "kinect/skeleton/0/foot_right");
//        oscP5.plug(this, "footLeft", "kinect/skeleton/0/foot_left");
//        oscP5.plug(this, "hipRight", "kinect/skeleton/0/hip_right");
//        oscP5.plug(this, "hipLeft", "kinect/skeleton/0/hip_left");
//        oscP5.plug(this, "hipCenter", "kinect/skeleton/0/hip_center");
//        oscP5.plug(this, "head", "kinect/skeleton/0/head");
        int len = stars.length;
        for (int i=0; i < len; i++) {
            stars[i] = new Star();
        }
        //pgl = (PGraphicsOpenGL) g;
        //gl = pgl.gl;
        //initVBO();
        
        // setup audio
        ac = new AudioContext();
        println(sketchPath(""));
        sourceAudioFile = sketchPath("") + "src\\data\\ding.wav";
        try {
        	// initialize our SamplePlayer, loading the file
        	// indicated by the sourceFile string
        	sp = new SamplePlayer(ac, new Sample(sourceAudioFile));
        }
        catch(Exception e)
        {
        	// If there is an error, show an error message
        	// at the bottom of the processing window.
        	println("Exception while attempting to load sample!");
        	e.printStackTrace(); // print description of the error
        	exit(); // and exit the program
        }
        sp.setKillOnEnd(false);
        // as usual, we create a gain that will control the volume
        // of our sample player
        gainValue = new Glide(ac, 0.0f, 20);
        g = new Gain(ac, 1, gainValue);
        g.addInput(sp); // connect the SamplePlayer to the Gain
        ac.out.addInput(g); // connect the Gain to the AudioContext
        ac.start(); // begin audio processing
        
        // initialize kinectData
        kinectData = new KinectData();
                
        polygonTarget = new PolygonTarget(this, 4, 650, -3000, colT, false, true, false);
        polygonPlayer = new PolygonPlayer(21, 200, yOffset, 0, colP, true, true, false);

        stage = new Box(this);
        stage.fill(color(stageColor));
        stage.stroke(color(lineColor));
        stage.strokeWeight(2f);
        stage.setSize(width*5f, height*2f, 4000);
        stage.moveTo(width/2, height/2, -1500);
        stage.drawMode(Shape3D.TEXTURE);
        //stage.drawMode(Shape3D.WIRE, Box.FRONT);
        //stage.drawMode(Shape3D.SOLID, Box.TOP);
        stage.setTexture("/src/data/KAMEN.jpg", Box.BOTTOM);
        //stage.setTexture("/src/data/KAMEN.jpg", Box.RIGHT);
        //stage.setTexture("/src/data/KAMEN.jpg", Box.LEFT);
        //stage.setTexture("/src/data/sky.jpg", Box.TOP);

        world = new Ellipsoid(this, 16 ,24);
        world.setTexture("/src/data/clouds.jpg");
        world.setRadius(4000, 4000, 2500);
        world.moveTo(width/2, height/2, -1000);
        world.rotateToY(-PI/2);
        world.drawMode(Shape3D.TEXTURE);
        //int color = color(255, 0, 255);
        //world.fill(color);

        worldOuter = new Ellipsoid(this, 16 ,24);
        worldOuter.setTexture("/src/data/clouds.jpg");
        worldOuter.setRadius(4000, 4000, 2000);
        worldOuter.moveTo(width/2, height/2, -1000);
        worldOuter.rotateToY(-PI/2.3f);
        worldOuter.drawMode(Shape3D.TEXTURE);
        //int color = color(255, 0, 255);
        //world.fill(color);
        
        
//        stage2 = new Box(this);
//        stage2.fill(color(stageColor));
//        stage2.stroke(color(lineColor));
//        stage2.strokeWeight(2f);
//        stage2.setSize(width*1.5f, height*2f, 4000);
//        stage2.rotateToY(PI/4);
//        stage2.moveTo(-width*9/8, height/2, -5900);
//        stage2.drawMode(Shape3D.TEXTURE);
//        stage2.drawMode(Shape3D.WIRE, Box.FRONT);
//        //stage.drawMode(Shape3D.SOLID, Box.TOP);
//        stage2.setTexture("/src/data/KAMEN-stup.jpg", Box.BOTTOM);
//        stage2.setTexture("/src/data/KAMEN.jpg", Box.RIGHT);
//        stage2.setTexture("/src/data/KAMEN.jpg", Box.LEFT);
//        stage2.setTexture("/src/data/sky.jpg", Box.TOP);

        /*
        terrain = new Terrain(this, 60, terrainSize, horizon);
        terrain.usePerlinNoiseMap(0, 40, 0.15f, 0.15f);
        terrain.moveTo(width/2, -height/2, 0);
        terrain.setTexture("/src/data/KAMEN.jpg", 4);
        terrain.tag = "Ground";
        terrain.tagNo = -1;
        terrain.drawMode(Shape3D.TEXTURE);
        float camSpeed = 10;
        TerrainCam cam = new TerrainCam(this);
        cam.adjustToTerrain(terrain, Terrain.WRAP, 8);
        cam.camera();
        cam.speed(camSpeed);
        cam.forward.set(cam.lookDir());
        */
        // setup obj import
//        try
//        {
//            scene = new ObjImpScene( this );
//            scene.load( dataPath("src\\data\\sponza\\sponza.obj"), 500 );
//    //  scene2.load( dataPath("spheres.obj"), .5 );
//        } catch( Exception e )
//        {
//            println( e );
//            System.exit( 0 );
//        }
        
        // setup the physics and the world
        physics = new VerletPhysics();
        //physics.setDrag(0.01f);
        physics.addBehavior(new GravityBehavior(new Vec3D(0, 1, 0)));
        //worldFloor = new VisibleBoxConstraint(new Vec3D(-width*4, height*1.5f, -3900), new Vec3D(width*4, height*1.55f, 1500));

        // init Gem
        gems = new Gem[4];
        for (int i = 0; i < gems.length; i++) {
        	gems[i] = new Gem(this, true, i * 500);
        }
        
        
        // MIDI
//        MidiBus.list();
//    	midiBus = new MidiBus(this, -1, "Java Sound Synthesizer");
    	
    	println(RWMidi.getOutputDeviceNames());
    	input = RWMidi.getInputDevices()[0].createInput(this);
    	output = RWMidi.getOutputDevices()[3].createOutput();

    	// timer to end notes after a given time outside the normal thread
    	midiTimer = new MidiTimer(0, 3);
    }
    
    class VisibleBoxConstraint extends BoxConstraint {

    	  public VisibleBoxConstraint(Vec3D min, Vec3D max) {
    	    super(min,max);
    	  }
    	  
    	  public void draw() {
    	    Vec3D m=box.getMin();
    	    Vec3D n=box.getMax();
    	    beginShape(QUAD_STRIP);
    	    stroke(255);
    	    fill(255, 0, 0);
    	    vertex(m.x,m.y,m.z); vertex(n.x,m.y,m.z);
    	    vertex(m.x,n.y,m.z); vertex(n.x,n.y,m.z);
    	    vertex(m.x,n.y,n.z); vertex(n.x,n.y,n.z);
    	    vertex(m.x,m.y,n.z); vertex(n.x,m.y,n.z);
    	    vertex(m.x,m.y,m.z); vertex(n.x,m.y,m.z);
    	    endShape();
    	  }
    	}
    
    public void mousePressed()
    {
    	/*
    	// set the gain based on mouse position
    	gainValue.setValue((float)mouseX/(float)width);
    	// move the playback pointer to the first loop point (0.0)
    	sp.setToLoopStart();
    	sp.start(); // play the audio file
    	*/
    }   
    
    public void draw() {
        //kinect.update();
    	physics.update();
        background(0);
        //directionalLight(51, 102, 126, -1, 0, 0);
        // draw the bg stars
//        pushMatrix();
//        translate(0,0,-1600);
//        drawStars();
//        popMatrix();
        
        
//        pushMatrix();
//        translate(width/2, height/2, 0);
//        fill(255,0,0,0);
//        //stroke(0);
//        int clr = color(255,0,0);
//        //drawBox(width * .75f, height * 1f, 1500, -1400, clr, false);
//        
//        //box(width * .75f, height * .75f, 1000);
//        popMatrix();
        
        fill(255,0,0, 0);

        // draw the game stage
        stage.draw();
        //worldFloor.draw();
        world.draw();
        //worldOuter.draw();
        //terrain.draw();
        
        // draw the game pieces
        if (wallComing) {
	        polygonTarget.updatePolygonPosition(polygonTarget.deltaX, polygonTarget.deltaY, polygonTarget.deltaZ);
	        polygonTarget.draw();
        } else {       	
        	boolean gemsDone = true;
            for (int i = 0; i < gems.length; i++) {
            	gems[i].draw();
                // if the gem has reached the player
    	        if (gems[i].zDepth >= polygonPlayer.zDepth) {
    		        // check to see if the player has caught a gem
		        	if (dist(gems[i].x, gems[i].y, polygonPlayer.handLeftX, polygonPlayer.handLeftY) < 120 ||
		        			dist(gems[i].x, gems[i].y, polygonPlayer.handRightX, polygonPlayer.handRightY) < 120) {
		        		polygonPlayer.gemScore++;
		        	}
		        } else {
		        	gemsDone = false;
		        }
            }
            if (gemsDone) wallComing = true;
        }

        polygonPlayer.drawPolygon();
        
        // if the target has reached the player
        if (polygonTarget.zDepth >= polygonPlayer.zDepth && !hitTest) {
            // check to see if hit target
            if (polygonPlayer.checkHit(polygonTarget)) {
                polygonPlayer.score++;
                hit = true;
            } else {
            	hit = false;
            }
            hitTest = true;
            sendMidi(0, 64, 100);
        }
        
        // keep drawing the wall past the player
        // once it gets past far enough regenerate and reset hitTest
        if (polygonTarget.zDepth > polygonPlayer.zDepth + 500) {
        	wallComing = false;
            hitTest = false;
            if (hit) {
                level++;
            }
        	polygonTarget.generatePolygon();
        	for (int i = 0; i < gems.length; i++) {
        		gems[i].initPosition();
        	}

        }
        
        
        renderHud();
        //text("Frame Rate: " + frameRate, scorePosX, 20, 0);
        //text("Score: " + polygonPlayer.score, scorePosX, 100, 0);

        // box.draw();
        
//        RG.shape(wall);
//        lights();
//        pushMatrix();
//        translate(width/2, height/2,0);
//        scene.draw();
//        popMatrix();

    //terrain.draw();
        
     
    }


    private void renderHud() {
    	fill(255);
    	text("Score: " + polygonPlayer.score + " :: Gems: " + polygonPlayer.gemScore, scorePosX, 100);
    }
    
    ShapeMover shapeMover;
    Box cloud;
    private void createFloatingShapes() {
    	cloud = new Box(this);
    	cloud.setSize(100, 100, 2);
    	cloud.moveTo(width/2, height/2, -1500);
    	cloud.setTexture("/src/data/clouds.png", Box.FRONT);
    	cloud.drawMode(Shape3D.TEXTURE);
    	shapeMover = new ShapeMover(this, cloud, new PVector(width/2, height/2, -500), new PVector(0,0,-200), 2f, 2f);
    }
    private void drawFloatingShapes() {
    	cloud.draw();
    }
    
    private void drawStars() {
        fill(255);
        stroke(128);
        int len = stars.length;
        for (int i=0; i < len; i++) {
            Star star = stars[i];
            star.draw();
        }

    }

    class Star{
        float size;
        float x;
        float y;

        Star() {
            size = (float)Math.random() * 10;
            x = ((float)Math.random() - 0.5f) * width * 4;
            y = ((float)Math.random() - 0.5f) * height * 4;
        }
        private void draw() {
            ellipse(x, y, size, size);
        }
    }
    
    class Gem{
        PApplet parent;
        float size;
        float xOff;
        float yOff;
        float zDepth, zOffset;
        float zSpeed = 30;
        float x,y,z;
        float increment = 0.01f;
        int gemRounds = 0;
        boolean complete = false;
        float r = 0;
        float pulseCounter = 0;
        float pulseIncrement = 3;
        boolean pulseDirectionOut = true;
        boolean physicsOn;
        boolean hasBounced = false;
        VerletParticle gemBall;
        MidiTimer gemTimer;
        
        Gem(PApplet p, boolean pOn, float zO) {
            parent = p;
            physicsOn = pOn;
            zOffset = zO;
            initPosition();
            gemTimer = new MidiTimer(0, 3);
        }
//        private void updatePosition() {
//            xOff += increment;
//            fill(255);
//            x = (parent.noise(xOff)*width * 0.75f) - width/8;
//            y = (parent.noise(yOff)*height) - height/4;
//             zDepth += ((float)Math.random() * 20) + 20;
//        }
//        private void initPosition() {
//            size = parent.random(10,50);
//            xOff = 0f + parent.random(width);
//            yOff = 20f + parent.random(height);
//            x = parent.noise(xOff)*width*1.5f;
//            y = parent.noise(yOff)*width;
//            zDepth = -2500;
//       }
        private void updatePosition() {
             if (physicsOn) {
	             x = gemBall.x;
	             y = gemBall.y;
	             z = gemBall.z;
             }
             //x += 10;
             //zDepth += 40;
             zDepth = z;
             
             //int v = (int)gemBall.getVelocity().magnitude();
             println(y);
             if (y >= 1040 && !hasBounced) {
            	 //int pitch = (int)Math.max(Math.abs(zDepth / 40), 36);
            	 int pitch = (int)map(Math.abs((x / 20)), 0, 100, 48, 80);
                 int v = (int)Math.min(gemBall.getVelocity().magnitude() * 2, 100);
                 //println("v: " + v);
            	 sendMidi(0, pitch, v);
            	 //println("x: " + x + ", y: " + y);
            	 //println("turned note " + pitch + " on");
            	 hasBounced = true;
            	 gemBall.addForce(new Vec3D(0,-100,0));
             }
             if (y < 1000 && hasBounced) {
            	 //println("bounce reset");
            	 hasBounced = false;
             }
        }
        private void initPosition() {
            pulseCounter = 0;
            pulseIncrement = 8;
            pulseDirectionOut = true;
            size = 10; //random(10,50);
            r = random(1);
            if (r > 0.5) {
            x = random(-width/4,0); // or parent.random(width, width*1.25f);
            } else {
                x = random(width, width*1.25f);               
            }
            y = random(-height, height/4); // or parent.random(height, height*1.5f);
            if (physicsOn) {
            	physics.removeParticle(gemBall);
	            gemBall = new VerletParticle(new Vec3D(x, y, zOffset - 5500));
	            gemBall.addForce(new Vec3D(0, 0, 50));
	            physics.addParticle(gemBall);
	            //VerletPhysics.addConstraintToAll(floor, physics.particles);
            }
            zDepth = zOffset - 5500;
            //gemBall.set(new Vec3D(width/2, height/2, -200));
            gemRounds++;
       }
        private void sendMidi(int channel, int pitch, int velocity) {
        	output.sendNoteOn(channel, pitch, velocity); // Send a Midi noteOn
        	gemTimer.setNote(channel, pitch, 250);
        }

        private void draw() {
            pulseCounter += pulseIncrement;
            if (pulseCounter > 30 && pulseDirectionOut) {
                pulseDirectionOut = false;
                pulseIncrement = -2;
            } else if (pulseCounter <= 0) {
                pulseDirectionOut = true;
                pulseIncrement = 2;
            }
            updatePosition();
            //directionalLight(51, 102, 126, -1, 0, 0);
            lights();
            pushMatrix();
            translate(x,y,zDepth);
            noStroke();
            fill(255, 0, 0);
            parent.sphere(size);
            fill(255, 255, 0, 80);
            parent.sphere(size + pulseCounter);
            fill(0, 80);
            translate(0,height*1.5f-y-10, 0);
            rotateX(PI/2);
            ellipse(0,0,size*4,size*4);
            popMatrix();
//            pushMatrix();
//            translate(0,0,zDepth);
//            rotateX(PI/2);
//            ellipse(0, height*3, 30,30);
//            popMatrix();
        }
    }

    private void sendMidi(int channel, int pitch, int velocity) {
    	output.sendNoteOn(channel, pitch, velocity); // Send a Midi noteOn
    	midiTimer.setNote(channel, pitch, 250);
    }

    
    // Joints are numbered:
    // going around clockwise from 
    // right foot to hip, wrist, shoulder, head, then other side
    
    // 1    hipCenter
	// 2    spine
	// 3    shoulderCenter
	// 4    head
	// 5    shoulderLeft
	// 6    elbowLeft
	// 7    wristLeft
	// 8    handLeft
	// 9    shoulderRight
	// 10   elbowRight
	// 11	wristRight
	// 12   handRight
	// 13   hipLeft
	// 14   kneeLeft
	// 15   ankleLeft
	// 16   footLeft
	// 17   hipRight
	// 18   kneeRight
	// 19   ankleRight
	// 20   footRight
    
    public void allJoints(String data) {
        fill(255);
        String[] joints = data.split(",");
        for(int i=0; i < joints.length; i++) {
        	String[] parts = joints[i].split(":");
        	String[] nameNumber = parts[0].split("\\|");
        	int jointNumber = Integer.parseInt(nameNumber[1].trim());
        	String name = nameNumber[0];
        	String[] values = parts[1].split("\\|");
        	float x = Float.valueOf(values[0].trim()).floatValue();
        	float y = Float.valueOf(values[1].trim()).floatValue();
        	float z = Float.valueOf(values[2].trim()).floatValue();
        	switch (jointNumber) {
	        	case 20:{
	        		polygonPlayer.updateJoint(0, x, y, z);
	        		polygonPlayer.updateJoint(1, x+.05f, y, z);
	        		break;
	        	}
	        	case 18:{
	        		polygonPlayer.updateJoint(2, x, y, z);
	        		break;
	        	}
	        	case 17:{
	        		polygonPlayer.updateJoint(3, x, y, z);
	        		break;
	        	}
	        	case 10:{
	        		polygonPlayer.updateJoint(5, x, y, z);
	        		break;
	        	}
	        	case 12:{
	        		polygonPlayer.updateJoint(6, x, y, z);
	        		polygonPlayer.updateJoint(7, x+.02f, y-.03f, z);
	        		break;
	        	}
	        	case 9:{
	        		polygonPlayer.updateJoint(4, x-.04f, y+.03f, z);
	        		polygonPlayer.updateJoint(8, x, y, z);
	        		break;
	        	}
	        	case 4:{
	        		polygonPlayer.updateJoint(9, x, y, z);
	        		break;
	        	}
	        	case 5:{
	        		polygonPlayer.updateJoint(10, x, y, z);
	        		polygonPlayer.updateJoint(14, x+.03f, y+.03f, z);
	        		break;
	        	}
	        	case 8:{
	        		polygonPlayer.updateJoint(11, x, y, z);
	        		polygonPlayer.updateJoint(12, x+.02f, y+.03f, z);
	        		break;
	        	}
	        	case 6:{
	        		polygonPlayer.updateJoint(13, x+.03f, y, z);
	        		break;
	        	}
	        	case 13:{
	        		polygonPlayer.updateJoint(15, x, y, z);
	        		break;
	        	}
	        	case 14:{
	        		polygonPlayer.updateJoint(16, x, y, z);
	        		break;
	        	}
	        	case 16:{
	        		polygonPlayer.updateJoint(17, x, y, z);
	        		polygonPlayer.updateJoint(18, x+.05f, y, z);
	        		break;
	        	}
	        	case 1:{
	        		polygonPlayer.updateJoint(19, x-.02f, y, z);
	        		polygonPlayer.updateJoint(20, x+.02f, y, z);
	        		break;
	        	}
        	}
        	        	
        	// text(name + " - " + "x: " + x*width + ", y: " + y*height, scorePosX, 100 + (i*20), 0);
        }
    }
        

//    // for debugging/testing purposes
//    public void mousePressed() {
//        /* create an osc message with address pattern /test */
//        OscMessage myMessage = new OscMessage("/test");
//
//        myMessage.add(123); /* add an int to the osc message */
//        myMessage.add(456); /* add a second int to the osc message */
//
//        /* send the message */
//        oscP5.send(myMessage, myRemoteLocation);
//    }
//
//
//    /* incoming osc message are forwarded to the oscEvent method. */
//    public void oscEvent(OscMessage theOscMessage) {
//        /* with theOscMessage.isPlugged() you check if the osc message has already been
//         * forwarded to a plugged method. if theOscMessage.isPlugged()==true, it has already
//         * been forward to another method in your sketch. theOscMessage.isPlugged() can
//         * be used for double posting but is not required.
//        */
//        //if(theOscMessage.isPlugged()==false) {
//        if (false) {
//            /* print the address pattern and the typetag of the received OscMessage */
//            println("### received an osc message.");
//            println("### addrpattern\t"+theOscMessage.addrPattern());
//            println("### typetag\t"+theOscMessage.typetag());
//        }
//    }

    // an attempt to create a box with transparency for OPENGL
    // not transparent in OPENGL, but does work in P3D
    public void drawBox(float sizeX, float sizeY, float sizeZ, float offsetZ, int clr, boolean filled) {
        pushMatrix();
        translate(0,0, offsetZ);
        scale(sizeX, sizeY, sizeZ);
        beginShape(QUADS);
        if (filled) {
        	fill(clr, 0);
        } else {
        	fill(clr, 0);
        }
        stroke(clr);
        strokeWeight(2f);
        //texture(tex);
        // front
        vertex(-1,  1,  1);
        vertex( 1,  1,  1);
        vertex( 1, -1,  1);
        vertex(-1, -1,  1);

        vertex( 1,  1,  1);
        vertex( 1,  1, -1);
        vertex( 1, -1, -1);
        vertex( 1, -1,  1);

        vertex( 1,  1, -1);
        vertex(-1,  1, -1);
        vertex(-1, -1, -1);
        vertex( 1, -1, -1);

        vertex(-1,  1, -1);
        vertex(-1,  1,  1);
        vertex(-1, -1,  1);
        vertex(-1, -1, -1);

        vertex(-1,  1, -1);
        vertex( 1,  1, -1);
        vertex( 1,  1,  1);
        vertex(-1,  1,  1);

        vertex(-1, -1, -1);
        vertex( 1, -1, -1);
        vertex( 1, -1,  1);
        vertex(-1, -1,  1);

        endShape();
        popMatrix();
    }

    public void drawSquareCutout(float sizeX, float sizeY, float offsetZ, int clr, boolean filled) {
        pushMatrix();
        translate(0,0, offsetZ);
        // scale(sizeX, sizeY, 1);
        
        float[][] adjustedPolygonCoords = polygonTarget.adjustedPolygonCoords;
        float[][] wall = polygonTarget.wall;
        int n = polygonTarget.numPoints;
        beginShape(QUADS);
        if (filled) {
        	fill(clr, 100);
        } else {
        	fill(clr, 0);
        }
        stroke(clr);
        //texture(tex);
        // front

//        vertex(-sizeX, -sizeY, 1);
//        vertex( sizeX, -sizeY, 1);
//        vertex( sizeX,  sizeY, 1);
//        vertex(-sizeX,  sizeY, 1);
//        vertex(-sizeX, -sizeY, 1);
        
        vertex(-sizeX/2, -sizeY/2, 1);
        vertex(-sizeX/2,  sizeY/2, 1);
        vertex( sizeX/2,  sizeY/2, 1);
        vertex( sizeX/2, -sizeY/2, 1);
        vertex(-sizeX/2, -sizeY/2, 1);

        //        vertex(wall[0][n-1], wall[1][n-1], -1);
//        for (int i=n-1; i > 0; i--) {
//            vertex(wall[0][i], wall[1][i], -1);
//            //curveVertex(adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i], adjustedPolygonCoords[2][i]);
//            //text(i,adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i] );
//        }
//        vertex(wall[0][n-1], wall[1][n-1], -1);


        endShape(CLOSE);
        popMatrix();
    }

    
    public boolean pointInPolygon(PolygonTarget polygonTarget, float[] p) {
        int i = 0;
        float[][] targetPoints =  polygonTarget.getPolygon();
        float[] pointsX = targetPoints[0];
        float[] pointsY = targetPoints[1];
        int polySides = targetPoints[0].length;
        int j = polySides - 1;
        float x = p[0];
        float y = p[1];
        boolean oddNodes = false;
        for (i = 0; i < polySides; i++) {
            if ((pointsY[i] < y && pointsY[j] >= y
                    || pointsY[j] < y && pointsY[i] >= y)
                    && (pointsX[i] <= x || pointsX[j] <= x)) {
                oddNodes ^= (pointsX[i] + (y - pointsY[i]) / (pointsY[j] - pointsY[i])
                        * (pointsX[j] - pointsX[i]) < x);
            }
            j = i;
        }
        return oddNodes;
    }

    
    class KinectData {
   	
    	KinectJoint hipCenter, spine, shoulderCenter, head, shoulderLeft, elbowLeft, wristLeft, handLeft,
			    	shoulderRight, elbowRight, wristRight, handRight, hipLeft, kneeLeft, ankleLeft, footLeft,
			    	hipRight, kneeRight, ankleRight, footRight;
    	
    	HashMap Joints;
    	
    	KinectData() {
            
    		Joints = new HashMap();
            hipCenter = new KinectJoint("HipCenter");
            spine = new KinectJoint("Spine");
            shoulderCenter = new KinectJoint("ShoulderCenter");
            head = new KinectJoint("Head");
            shoulderLeft = new KinectJoint("ShoulderLeft");
            elbowLeft = new KinectJoint("ElbowLeft");
            wristLeft = new KinectJoint("WristLeft");
            handLeft = new KinectJoint("HandLeft");
            shoulderRight = new KinectJoint("ShoulderRight");
            elbowRight = new KinectJoint("ElbowRight");
            wristRight = new KinectJoint("WristRight");
            handRight = new KinectJoint("HandRight");
            hipLeft = new KinectJoint("HipLeft");
            kneeLeft = new KinectJoint("KneeLeft");
            ankleLeft = new KinectJoint("AnkleLeft");
            footLeft = new KinectJoint("FootLeft");
            hipRight = new KinectJoint("HipRight");
            kneeRight = new KinectJoint("KneeRight");
            ankleRight = new KinectJoint("AnkleRight");
            footRight = new KinectJoint("FootRight");
    		Joints.put("HipCenter", hipCenter);
    		Joints.put("Spine", spine);
    		Joints.put("HipCenter", hipCenter);
    		Joints.put("ShoulderCenter", shoulderCenter);
    		Joints.put("Head", head);
    		Joints.put("ShoulderLeft", shoulderLeft);
    		Joints.put("ElbowLeft", elbowLeft);
    		Joints.put("WristLeft", wristLeft);
    		Joints.put("HandLeft", handLeft);
    		Joints.put("ShoulderRight", shoulderRight);
    		Joints.put("ElbowRight", elbowRight);
    		Joints.put("WristRight", wristRight);
    		Joints.put("HandRight", handRight);
    		Joints.put("HipLeft", hipLeft);
    		Joints.put("KneeLeft", kneeLeft);
    		Joints.put("AnkleLeft", ankleLeft);
    		Joints.put("FootLeft", footLeft);
    		Joints.put("HipRight", hipRight);
    		Joints.put("KneeRight", kneeRight);
    		Joints.put("AnkleRight", ankleRight);
    		Joints.put("FootRight", footRight);         		
    	}
  	
    }

    
    class KinectJoint {
    	public float x, y, z;
    	String name;
    	
    	KinectJoint(String n) {
    		name = n;
    		x = 0;
    		y = 0;
    		z = 0;
    	}
    	
    }

    class PolygonTarget {
        float[][] polygon;
        float[][] wall;
        int clr;
        boolean move, kinect, filled;
        int numPoints;
        float[][] adjustedPolygonCoords;
        float zDepth, ogDepth;
        float wallThickness;
        int polySize;
        float deltaX, deltaY, deltaZ;
        int score, lives;
        shapes3d.Box boxTarget;
        PApplet parent;
        float wallSize;
        float bottomRightX, topRightX, topRightY, topLeftX, topLeftY, bottomLeftX;
        float bottomRightU, topRightU, topRightV, topLeftU, topLeftV, bottomLeftU;

        PolygonTarget(PApplet p, int n, int size, float z, int c, boolean m, boolean k, boolean f) {
        	parent = p;
            zDepth = ogDepth = z;
            wallThickness = 100;
            polySize = size;
            polygon = new float[3][n];
            adjustedPolygonCoords = new float[3][n];
            wall = simpleWall();
            clr = c;
            move = m;
            kinect = k;
            filled = f;
            numPoints = n;
            score = 0;
            lives = 5;
            wallSize = resetWallSize();
            
            boxTarget = new Box(kinectris32.Kinectris32.this);
            boxTarget.fill(color(stageColor));
            boxTarget.stroke(color(lineColor));
            boxTarget.strokeWeight(2f);
            boxTarget.setSize(width * 1.5f, height*2f, 10);
            boxTarget.moveTo(width/2, height/2, 0);
            boxTarget.drawMode(Shape3D.TEXTURE);
            boxTarget.setTexture("/src/data/KAMEN.jpg", Box.FRONT);
            boxTarget.setTexture("/src/data/KAMEN-stup.jpg", Box.LEFT);
            boxTarget.setTexture("/src/data/KAMEN-stup.jpg", Box.RIGHT);
            generatePolygon();
        }

        private void resetDeltas() {
        	zDepth = ogDepth;
            deltaX = ((float)Math.random() * 6) - 3 ;
            deltaY = ((float)Math.random() * 6) - 3;
            deltaZ = 30; //((float)Math.random() * 20) + 20;
        }
        
        private float resetWallSize() {
        	float size = parent.random(.5f,5);
        	return size;
        }

        private float[][] simpleWall() {
        	//this.zDepth = z;
        	wallSize = resetWallSize();
        	float[][] polygonCoords = new float[3][4];
        	float z = zDepth;
        	//leftEdge, -height/2, boxTarget.getWidth(), boxTarget.getHeight()
            polygonCoords[0][0] = leftEdge;
            polygonCoords[1][0] = -height/2;
            polygonCoords[2][0] = z;
            polygonCoords[0][1] = width/wallSize - width/4;
            polygonCoords[1][1] = -height/2;
            polygonCoords[2][1] = z;
            polygonCoords[0][2] = width/wallSize - width/4;
            polygonCoords[1][2] = height*1.5f;
            polygonCoords[2][2] = z;
            polygonCoords[0][3] = leftEdge;
            polygonCoords[1][3] = height*1.5f;
            polygonCoords[2][3] = z;
            return polygonCoords;
        }
        
        
        
        private float[][] polygonPathConvexRelative(int n, int size, float z) {
            this.zDepth = z;
            float[][] polygonCoords = new float[3][n];
            double angle = 2 * Math.PI / n;
            float startX = 0;
            float startY = (float)Math.random() * size;
            polygonCoords[0][0] = startX;
            polygonCoords[1][0] = height;
            polygonCoords[2][0] = z;
            for (int i=1; i < n-1; i++) {
                float newX = (float)Math.sin(angle * i) * size * (float)Math.random();
                float newY = (float)Math.cos(angle * i) * size * (float)Math.random();
                polygonCoords[0][i] = newX;
                polygonCoords[1][i] = newY;
                polygonCoords[2][i] = z;
            }
            polygonCoords[0][n-1] = startX - (float)Math.random() * size;
            polygonCoords[1][n-1] = height;
            polygonCoords[2][n-1] = z;
            // polygonCoords is array of array of coords
            // [[x1, x2, x3, x4...], [y1, y2, y3, y4]]
            return polygonCoords;
        }

        private float[][] getPolygon() {
            return adjustedPolygonCoords;
        }

        private void generatePolygon() {
        	//println(level);
        	switch (level) {
        	case 1:
            	generateWall();
        		break;
        	case 2:
        		generateWallCutout();
        		//polygon = polygonPathConvexRelative(numPoints, polySize, ogDepth);
        		break;
        	default:
        		generateWallCutout();
        		//polygon = polygonPathConvexRelative(numPoints, polySize, ogDepth);
        		break;
        	}
        	
        		
//            if (level == 2) {
//            	polygon = polygonPathConvexRelative(numPoints, polySize, ogDepth);
//            } else {
//            	polygon = simpleWall(ogDepth);  
//            }            
            //offsetAdjustedPolygonCoords();
            resetDeltas();
        }

        private void updatePolygonPosition(float deltaX, float deltaY, float deltaZ) {
            float newZ;
            zDepth += deltaZ;
        	if (polygon.length != 0) {
	            if (numPoints > 4) {
		            for (int i = 0; i < numPoints; i++) {
		                polygon[0][i] += deltaX;
		                polygon[1][i] += deltaY;
		                polygon[2][i] = zDepth;
		            }
	            } else {
		            for (int i = 0; i < numPoints; i++) {
		                polygon[2][i] = zDepth;
		            }          	
	            }
        	}
        }

        private void offsetAdjustedPolygonCoords() {
        	if (polygon[0].length != 0) {
	            float mX = 0;
	            float mY = 0;
	            float offsetX = width/2;
	            float offsetY = height/2;
	            float offsetZ = 0;
//	            if (move) {
//	                mX = mouseX * 1.5f;
//	                mY = mouseY * 1.5f;
//	                offsetX = 0;
//	                offsetY = 0;
//	            }
	            if (kinect) {
	                mX = 0;
	                mY = 0;
	                offsetX = 0;
	                offsetY = 0;
	            }
	            for (int i=0; i < numPoints; i++) {
	                adjustedPolygonCoords[0][i] = polygon[0][i] + offsetX + mX;
	                adjustedPolygonCoords[1][i] = polygon[1][i] + offsetY + mY;
	                adjustedPolygonCoords[2][i] = polygon[2][i] + offsetZ;
	            }
        	}
        }

        private void drawWall() {
            switch (level) {
        	case 1:
                strokeWeight(2f);
            	int n = 4;
                if (polygon.length != 0) {
                	n = polygon[0].length;
                }
                beginShape();
                for (int i = 0; i < n; i++) {
                	vertex(polygon[0][i], polygon[1][i], polygon[2][i]);
                }
                endShape(CLOSE);
                strokeWeight(1);
                fill(0);
        		
                float newWidth = width/wallSize;
                boxTarget.setSize(newWidth, height*2f, 50);
                boxTarget.moveTo(leftEdge + newWidth/2, height/2, zDepth-10);
                boxTarget.draw();  
        		break;
        	case 2:
        		drawWallWithCutout();
        		//polygon = polygonPathConvexRelative(numPoints, polySize, ogDepth);
        		break;
        	default:
        		drawWallWithCutout();
        		//polygon = polygonPathConvexRelative(numPoints, polySize, ogDepth);
        		break;
        	}
        }
        
        private void generateWall() {
        	//wallSize = resetWallSize();
    		polygon = simpleWall();
            offsetAdjustedPolygonCoords();
        }
        
        private void generateWallCutout() {
            bottomRightX = parent.random(width/1.5f, width);
            topRightX = parent.random(width/1.5f, width);
            topRightY = parent.random(0, height/2);
            topLeftX = parent.random(0, width/2.5f);
            topLeftY = parent.random(0, height/2);
            bottomLeftX = parent.random(0, width/2.5f);
            bottomRightU = ((2 * bottomRightX) / (3 * width)) + .1666f;
            topRightU = ((2 * topRightX) / (3 * width)) + .1666f;
            topRightV = ((topRightY / 2) / height) + .25f;
            topLeftU = ((2 * topLeftX) / (3 * width)) + .1666f;
            topLeftV = ((topLeftY / 2) / height) + .25f;
            bottomLeftU = ((2 * bottomLeftX) / (3 * width)) + .1666f;
            
        	float[][] polygonCoords = new float[3][4];
        	float z = zDepth;
        	//leftEdge, -height/2, boxTarget.getWidth(), boxTarget.getHeight()
            polygon[0][0] = bottomRightX;
            polygon[1][0] = bottomEdge;
            polygon[2][0] = ogDepth;
            polygon[0][1] = topRightX;
            polygon[1][1] = topRightY;
            polygon[2][1] = ogDepth;
            polygon[0][2] = topLeftX;
            polygon[1][2] = topLeftY;
            polygon[2][2] = ogDepth;
            polygon[0][3] = bottomLeftX;
            polygon[1][3] = bottomEdge;
            polygon[2][3] = ogDepth;
            
            offsetAdjustedPolygonCoords();
           
        }
        
        private void drawWallWithCutout() {
        	// draw front part of the wall
        	noStroke();
        	beginShape();
            textureMode(NORMALIZED);
            texture(tex);
            vertex(leftEdge, topEdge, zDepth,0,0);
            vertex(rightEdge, topEdge, zDepth,1,0);
            vertex(rightEdge, bottomEdge, zDepth,1,1);
            // begin cutout
            vertex(bottomRightX, bottomEdge, zDepth,bottomRightU,1);
            vertex(topRightX, topRightY, zDepth,topRightU,topRightV);
            vertex(topLeftX, topLeftY, zDepth,topLeftU, topLeftV);
            vertex(bottomLeftX, bottomEdge, zDepth,bottomLeftU,1);
            // end cutout
            vertex(leftEdge, bottomEdge, zDepth,0,1);
            endShape(CLOSE);
            
            // draw back part of the wall
            float zThickDepth = zDepth - wallThickness;
        	beginShape();
            textureMode(NORMALIZED);
            texture(tex);
            vertex(leftEdge, topEdge, zThickDepth,0,0);
            vertex(rightEdge, topEdge, zThickDepth,1,0);
            vertex(rightEdge, bottomEdge, zThickDepth,1,1);
            // begin cutout
            vertex(bottomRightX, bottomEdge, zThickDepth,bottomRightU,1);
            vertex(topRightX, topRightY, zThickDepth,topRightU,topRightV);
            vertex(topLeftX, topLeftY, zThickDepth,topLeftU, topLeftV);
            vertex(bottomLeftX, bottomEdge, zThickDepth,bottomLeftU,1);
            // end cutout
            vertex(leftEdge, bottomEdge, zThickDepth,0,1);
            endShape(CLOSE);
            
            // draw cutout internal surfaces
            //right
            beginShape();
            textureMode(NORMALIZED);
            texture(tex);
            vertex(bottomRightX, bottomEdge, zDepth,.1f,1);
            vertex(bottomRightX, bottomEdge, zThickDepth,0,1);
            vertex(topRightX, topRightY, zThickDepth,0,0);
            vertex(topRightX, topRightY, zDepth,.1f,0);
            endShape(CLOSE);
            //top
            beginShape();
            textureMode(NORMALIZED);
            texture(tex);
            vertex(topRightX, topRightY, zDepth,.1f,1);
            vertex(topRightX, topRightY, zThickDepth,0,1);
            vertex(topLeftX, topLeftY, zThickDepth,0,0);
            vertex(topLeftX, topLeftY, zDepth,.1f, 0);
            endShape(CLOSE);
            //left
            beginShape();
            textureMode(NORMALIZED);
            texture(tex);
            vertex(topLeftX, topLeftY, zDepth,.1f,0);
            vertex(topLeftX, topLeftY, zThickDepth,0,0);
            vertex(bottomLeftX, bottomEdge, zThickDepth,0,1);
            vertex(bottomLeftX, bottomEdge, zDepth,.1f,1);
            endShape(CLOSE);
            //floor shadow
            fill(0, 80);
            /*pushMatrix();
            translate(0,height*1.5f, zDepth);
            rotateX(PI/2);
            rect(bottomLeftX,bottomEdge, wallThickness, bottomRightX - bottomLeftX);
            popMatrix();
            */
            beginShape();
            vertex(bottomLeftX, bottomEdge-5, zDepth+20);
            vertex(bottomLeftX, bottomEdge-5, zThickDepth-20);
            vertex(bottomRightX, bottomEdge-5, zThickDepth-20);
            vertex(bottomRightX, bottomEdge-5, zDepth+20);
            endShape(CLOSE);
            
            // hit Polygon - don't need to draw it
            strokeWeight(2f);
            stroke(255);
        	int n = 4;
            if (polygon.length != 0) {
            	n = polygon[0].length;
            }
            beginShape();
            for (int i = 0; i < n; i++) {
            	vertex(polygon[0][i], polygon[1][i], polygon[2][i]);
            }
            endShape(CLOSE);
            strokeWeight(1);
            fill(0);

            
        }
        
        private void draw() {
            // polygon is array of array of coords
            // [[x1, x2, x3, x4...], [y1, y2, y3, y4]]
            fill(255, 100);
            stroke(255);
            //noStroke();
            drawWall();
            

        }

    }
    
    class PolygonPlayer {
        float[][] polygon;
        int clr;
        boolean move, kinect, filled;
        int numPoints;
        float[][] adjustedPolygonCoords;
        float zDepth, ogDepth, yOffset;;
        int polySize;
        float deltaX, deltaY, deltaZ;
        int score, gemScore, lives;
        Box boxPlayer;
        float handLeftX, handLeftY, handRightX, handRightY;
        float footLeftY, footRightY, floorOffset;
        
        PolygonPlayer(int n, int size, float yO, float z, int c, boolean m, boolean k, boolean f) {
            zDepth = ogDepth = z;
            yOffset = yO;
            polySize = size;
            adjustedPolygonCoords = new float[3][n];
            clr = c;
            move = m;
            kinect = k;
            filled = f;
            numPoints = n;
            score = 0;
            gemScore = 0;
            handLeftX = 0;
            handLeftY = 0;
            footLeftY = 0;
            footRightY = 0;
            floorOffset = 0;
            lives = 5;
            boxPlayer = new Box(kinectris32.Kinectris32.this);
            boxPlayer.fill(color(stageColor));
            boxPlayer.stroke(color(lineColor));
            boxPlayer.strokeWeight(2f);
            boxPlayer.setSize(width * 1.5f, height*2f, 4);
            boxPlayer.moveTo(width/2, height/2, 0);
            boxPlayer.drawMode(Shape3D.WIRE);
            generatePolygon();
        }
        
        private float[][] polygonPathConvexRelative(int n, int size, float z) {
            this.zDepth = z;
            float[][] polygonCoords = new float[3][n];
            double angle = 2 * Math.PI / n;
            float startX = 0;
            float startY = (float)Math.random() * size;
            polygonCoords[0][0] = startX;
            polygonCoords[1][0] = startY;
            polygonCoords[2][0] = z;
            for (int i=1; i < n; i++) {
                float newX = (float)Math.sin(angle * i) * size * (float)Math.random();
                float newY = (float)Math.cos(angle * i) * size * (float)Math.random();
                polygonCoords[0][i] = newX;
                polygonCoords[1][i] = newY;
                polygonCoords[2][i] = z;
            }
            // polygonCoords is array of array of coords
            // [[x1, x2, x3, x4...], [y1, y2, y3, y4]]
            return polygonCoords;
        }

        private float[][] getPolygon() {
            return adjustedPolygonCoords;
        }

        private void generatePolygon() {
            polygon = polygonPathConvexRelative(numPoints, polySize, ogDepth);         
            offsetAdjustedPolygonCoords();
        }

        private void offsetAdjustedPolygonCoords() {
            float mX = 0;
            float mY = 0;
            float offsetX = width/2;
            float offsetY = height;
            float offsetZ = 0;
            if (move) {
                mX = mouseX;
                mY = mouseY;
                offsetX = 0;
                offsetY = 0;
            }
            if (kinect) {
                mX = 0;
                mY = 0;
                offsetX = 0;
                offsetY = 0;
            }
            for (int i=0; i < numPoints; i++) {
                adjustedPolygonCoords[0][i] = polygon[0][i] + offsetX + mX;
                adjustedPolygonCoords[1][i] = polygon[1][i] + offsetY + mY + yOffset;
                adjustedPolygonCoords[2][i] = polygon[2][i] + offsetZ;
            }
        }

        private void drawPolygon() {
            // polygon is array of array of coords
            // [[x1, x2, x3, x4...], [y1, y2, y3, y4]]
            int n = polygon[0].length;
            offsetAdjustedPolygonCoords();
//            fill(clr, 20);
//            stroke(clr, 50);
//            pushMatrix();
//            translate(width/2, height/2, 0);
//            //box(width * .75f, height * .75f, 2);
//            //drawBox(width * .75f, height * 1f, 1f, zDepth, clr, false);
//            popMatrix();
            boxPlayer.moveTo(width/2, height/2, zDepth-20);
            boxPlayer.draw();

            if (filled) {
                fill(clr);
            } else {
                noFill();
            }
            stroke(clr);
            strokeWeight(4f);
            footRightY = adjustedPolygonCoords[1][0];
            footLeftY = adjustedPolygonCoords[1][17];
            floorOffset = Math.min(height*1.5f - footRightY, height*1.5f - footLeftY);
            pushMatrix();
            translate(0, floorOffset, 0);
            beginShape();
            curveVertex(adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0], adjustedPolygonCoords[2][0]+1);
            for (int i=0; i < n; i++) {
                //vertex(adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i], adjustedPolygonCoords[2][i], adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i]);
                curveVertex(adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i], adjustedPolygonCoords[2][i]+1);
                //text(i,adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i] );
            }
            //vertex(adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0], adjustedPolygonCoords[2][0], adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0]);
            curveVertex(adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0], adjustedPolygonCoords[2][0]+1);
            //text(n-1,adjustedPolygonCoords[0][n-1], adjustedPolygonCoords[1][n-1] );
            endShape(CLOSE);
            popMatrix();
            handLeftX = adjustedPolygonCoords[0][11];
            handLeftY = adjustedPolygonCoords[1][11] + floorOffset;
            handRightX = adjustedPolygonCoords[0][6];
            handRightY = adjustedPolygonCoords[1][6] + floorOffset;
        	fill(0,0);
        	strokeWeight(2f);
        	stroke(255,255,0,120);
        	pushMatrix();
        	translate(handLeftX, handLeftY, zDepth-15);
        	ellipse(0,0, 60, 60);
            popMatrix();
        	pushMatrix();
        	translate(handRightX, handRightY, zDepth-15);
        	ellipse(0,0, 60, 60);
            popMatrix();
            strokeWeight(1f);
            if (debug) {
                for (int i=0; i < n; i++) {
                    text(i,adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i], adjustedPolygonCoords[2][i] );
                }
                text(n-1,adjustedPolygonCoords[0][n-1], adjustedPolygonCoords[1][n-1],adjustedPolygonCoords[2][n-1] );
            }

        }

        private boolean checkHit(PolygonTarget polygonTarget) {
            int numPlayerPoints = this.numPoints;
            boolean hit = false;
            for (int p = 0; p < numPlayerPoints; p++) {
                float[] point = {this.getPolygon()[0][p], this.getPolygon()[1][p]};
                if (pointInPolygon(polygonTarget, point)) {
                    hit = true;
                } else {
                    hit = false;
                    break;
                }
            }
            return hit;
        }

        private void updateJoint(int joint, float x, float y, float z) {
            // adjust x,y,z positions to match canvas dimensions
            x = (x * width * playerScaleFactor) -width/4;
            y = (y * height * playerScaleFactor) - height/4;
            polygon[0][joint] = x;
            polygon[1][joint] = y;
            // leave z alone for now
            // polygon[2][joint] = z;
            offsetAdjustedPolygonCoords();
        }
    }

	public static void main(String _args[]) {
		PApplet.main(new String[] { kinectris32.Kinectris32.class.getName() });
	}
}
