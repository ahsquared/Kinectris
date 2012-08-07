package kinectris32;

import java.util.HashMap;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import peasy.PeasyCam;
import javax.media.opengl.*;
import processing.opengl.*;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import shapes3d.Box;
import shapes3d.Shape3D;
import shapes3d.utils.*;
import shapes3d.animation.*;
import shapes3d.*;
import beads.AudioContext;
import beads.Gain;
import beads.Glide;
import beads.WavePlayer;
import objimp.*;

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
    boolean targetComing = true;
    PFont myFont;
    int scorePosX = 100;
    boolean paused, hit;
    int pauseCounter = 0;
    Star [] stars = new Star[200];
    Gem gem = new Gem(this);
    
    // position of player on screen
    float yOffset = 0;
    float playerScaleFactor = 1.5f;
    
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
    float baseFrequency = 200.0f;
    int sineCount = 10;
    WavePlayer sineTone[];
    Gain sineGain[];
    Gain masterGain;
    Glide sineFrequency[];
    
    PImage tex;
    
    PeasyCam camera;
    
    Box box, stage, boxTarget;
    
    int stageColor = color(186, 0, 203);
    int lineColor = color(235, 221, 0);
//    RShape wall;
//    RStyle wallStyle;
    
    public void setup() {
        size(1024, 768, OPENGL);
        float fov = PI/2.5f;
        float cameraZ = (height/2.0f) / tan(PI*60.0f/360.0f);
        perspective(fov, width/height, cameraZ/10.0f, cameraZ*10.0f);
        //perspective(radians(45), width/height, 10f,150000f);
        frameRate(30);
        myFont = createFont("Arial", 32);
        textFont(myFont);
        fill(0);
        hint(ENABLE_NATIVE_FONTS);
        textMode(SCREEN);
        camera = new PeasyCam(this, 510, 385, 0, 1150);
        
        tex = loadImage("/src/data/chr.jpg");
        
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
        
        // setup audio kinect
        ac = new AudioContext();
        masterGain = new Gain(ac, 1, 0.5f);
        ac.out.addInput(masterGain);

        // initialize kinectData
        kinectData = new KinectData();
                
        polygonTarget = new PolygonTarget(4, 650, -1800, colT, false, false, false);
        polygonPlayer = new PolygonPlayer(21, 200, yOffset, 0, colP, true, true, false);

        stage = new Box(this);
        stage.fill(color(stageColor));
        stage.stroke(color(lineColor));
        stage.strokeWeight(2f);
        stage.setSize(width*1.5f, height*2f, 4000);
        stage.moveTo(width/2, height/2, -1900);
        stage.drawMode(Shape3D.TEXTURE);
        stage.drawMode(Shape3D.WIRE, Box.FRONT);
        //stage.drawMode(Shape3D.SOLID, Box.TOP);
        stage.setTexture("/src/data/KAMEN-stup.jpg", Box.BOTTOM);
        stage.setTexture("/src/data/KAMEN.jpg", Box.RIGHT);
        stage.setTexture("/src/data/KAMEN.jpg", Box.LEFT);
        stage.setTexture("/src/data/sky.jpg", Box.TOP);

        terrain = new Terrain(this, 60, terrainSize, horizon);
        terrain.usePerlinNoiseMap(0, 40, 0.15f, 0.15f);
        terrain.setTexture("grass2.jpg", 4);
        terrain.tag = "Ground";
        terrain.tagNo = -1;
        terrain.drawMode(Shape3D.TEXTURE);
        
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
    }
    
    public void draw() {
        //kinect.update();

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
        
        fill(255,0,0, 200);

        // draw the game stage
        stage.draw();

        
        // draw the game pieces
        if (targetComing) {
	        polygonTarget.updatePolygonPosition(polygonTarget.deltaX, polygonTarget.deltaY, polygonTarget.deltaZ);
	        polygonTarget.drawPolygon();
        } else {
        	gem.draw();
        }
        polygonPlayer.drawPolygon();
        
        // if the target has reached the player
        if (polygonTarget.zDepth >= polygonPlayer.zDepth) {
            // check to see if hit target
            if (polygonPlayer.checkHit(polygonTarget)) {
                polygonPlayer.score++;
                level++;
                hit = true;
            } else {
            	hit = false;
            }
            targetComing = false;
            polygonTarget.generatePolygon();
        }
        
        // if the gem has reached the player
        if (gem.zDepth >= polygonPlayer.zDepth) {
        // check to see if the player has caught a gem
        	if (dist(gem.x, gem.y, polygonPlayer.handLeftX, polygonPlayer.handLeftY) < 120 ||
        			dist(gem.x, gem.y, polygonPlayer.handRightX, polygonPlayer.handRightY) < 120) {
        		polygonPlayer.gemScore++;
        	}
        	if (!targetComing) gem.initPosition();
        	if (gem.gemRounds % 4 == 0) {
        		targetComing = true;
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
    	float zDepth;
    	float zSpeed = 30;
    	float x,y;
    	float increment = 0.01f;
    	int gemRounds = 0;
    	float r = 0;
    	
    	Gem(PApplet p) {
    		parent = p;
    		x = random(-width/4,0);
			initPosition();
        }
//    	private void updatePosition() {
//    		xOff += increment;
//			fill(255);
//            x = (parent.noise(xOff)*width * 0.75f) - width/8;
//            y = (parent.noise(yOff)*height) - height/4;
//     		zDepth += ((float)Math.random() * 20) + 20;
//    	}
//    	private void initPosition() {
//            size = parent.random(10,50);
//            xOff = 0f + parent.random(width);
//            yOff = 20f + parent.random(height);
//            x = parent.noise(xOff)*width*1.5f;
//            y = parent.noise(yOff)*width;
//            zDepth = -2500;
//   	}
    	private void updatePosition() {
     		zDepth += ((float)Math.random() * zSpeed) + 20;
    	}
    	private void initPosition() {
            size = random(10,50);
            r = random(1);
            if (r > 0.5) {
            x = random(-width/4,0); // or parent.random(width, width*1.25f);
            } else {
                x = random(width, width*1.25f);            	
            }
            y = random(-height/2, height*1.5f); // or parent.random(height, height*1.5f);
            zDepth = -2500;
            gemRounds++;
   	}
        private void draw() {
        	updatePosition();
        	//directionalLight(51, 102, 126, -1, 0, 0);
        	lights();
        	pushMatrix();
        	translate(x,y,zDepth);
        	noStroke();
        	fill(255, 0, 0);
            parent.sphere(size);
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
        int polySize;
        float deltaX, deltaY, deltaZ;
        int score, lives;
        shapes3d.Box boxTarget;
        
        PolygonTarget(int n, int size, float z, int c, boolean m, boolean k, boolean f) {
            zDepth = ogDepth = z;
            polySize = size;
            adjustedPolygonCoords = new float[3][n];
            wall = simpleWall(ogDepth);
            clr = c;
            move = m;
            kinect = k;
            filled = f;
            numPoints = n;
            score = 0;
            lives = 5;

            boxTarget = new Box(kinectris32.Kinectris32.this);
            boxTarget.fill(color(stageColor));
            boxTarget.stroke(color(lineColor));
            boxTarget.strokeWeight(2f);
            boxTarget.setSize(width * 1.5f, height*2f, 10);
            boxTarget.moveTo(width/2, height/2, 0);
            boxTarget.drawMode(Shape3D.TEXTURE | Shape3D.WIRE, Box.FRONT);
            boxTarget.setTexture("/src/data/KAMEN-stup.jpg", Box.FRONT);
            generatePolygon();
        }

        private void resetDeltas() {
            deltaX = ((float)Math.random() * 6) - 3 ;
            deltaY = ((float)Math.random() * 6) - 3;
            deltaZ = ((float)Math.random() * 20) + 20;
        }
        
        private float[][] simpleWall(float z) {
        	this.zDepth = z;
        	float[][] polygonCoords = new float[3][4];
        	float wallWidth = ((float)Math.random() * (width-300)) - (width*.75f) + 300;
            polygonCoords[0][0] = wallWidth;
            polygonCoords[1][0] = -height;
            polygonCoords[2][0] = z;
            polygonCoords[0][1] = wallWidth;
            polygonCoords[1][1] = height;
            polygonCoords[2][1] = z;
            polygonCoords[0][2] = -width * .75f;
            polygonCoords[1][2] = height;
            polygonCoords[2][2] = z;
            polygonCoords[0][3] = -width * .75f;
            polygonCoords[1][3] = -height;
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
        	switch (level) {
        	case 1:
        		polygon = simpleWall(ogDepth);
        		break;
        	case 2:
        		polygon = polygonPathConvexRelative(numPoints, polySize, ogDepth);
        		break;
        	default:
        		polygon = polygonPathConvexRelative(numPoints, polySize, ogDepth);
        		break;
        	}
        	
        		
//            if (level == 2) {
//            	polygon = polygonPathConvexRelative(numPoints, polySize, ogDepth);
//            } else {
//            	polygon = simpleWall(ogDepth);  
//            }            
            offsetAdjustedPolygonCoords();
            resetDeltas();
        }

        private void updatePolygonPosition(float deltaX, float deltaY, float deltaZ) {
            float newZ;
            zDepth += deltaZ;
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

        private void offsetAdjustedPolygonCoords() {
            float mX = 0;
            float mY = 0;
            float offsetX = width/2;
            float offsetY = height/2;
            float offsetZ = 0;
            if (move) {
                mX = mouseX * 1.5f;
                mY = mouseY * 1.5f;
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
                adjustedPolygonCoords[1][i] = polygon[1][i] + offsetY + mY;
                adjustedPolygonCoords[2][i] = polygon[2][i] + offsetZ;
            }
        }

        private void drawPolygon() {
            // polygon is array of array of coords
            // [[x1, x2, x3, x4...], [y1, y2, y3, y4]]
            int n = polygon[0].length;
            offsetAdjustedPolygonCoords();
            fill(clr, 0);
            stroke(clr, 50);
            boxTarget.moveTo(width/2, height/2, zDepth-10);
            boxTarget.setSize(width/5, height, 1);
            boxTarget.draw();
            strokeWeight(2);
            fill(0);
//            beginShape();
//            texture(tex);
//            //vertex(adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0], adjustedPolygonCoords[2][0]);
//            //curveVertex(adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0], adjustedPolygonCoords[2][0]);
//            vertex(adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0], adjustedPolygonCoords[2][0]); //, adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0]);
//            for (int i=0; i < n; i++) {
//                vertex(adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i], adjustedPolygonCoords[2][i]); //, adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i]);
//                //curveVertex(adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i], adjustedPolygonCoords[2][i]);
//                //text(i,adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i] );
//            }
//            vertex(adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0], adjustedPolygonCoords[2][0]); //, adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0]);
//            //curveVertex(adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0], adjustedPolygonCoords[2][0]);
//            //text(n-1,adjustedPolygonCoords[0][n-1], adjustedPolygonCoords[1][n-1] );
//            endShape(CLOSE);
            strokeWeight(1);
            fill(0);
            if (debug) {
                for (int i=0; i < n; i++) {
                    text(i,adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i], adjustedPolygonCoords[2][i] );
                }
                text(n-1,adjustedPolygonCoords[0][n-1], adjustedPolygonCoords[1][n-1],adjustedPolygonCoords[2][n-1] );
            }

        }

        private void updateJoint(int joint, float x, float y, float z) {
            // adjust x,y,z positions to match canvas dimensions
            x = (x * width);
            y = (y * height);
            polygon[0][joint] = x;
            polygon[1][joint] = y;
            // leave z alone for now
            // polygon[2][joint] = z;
            offsetAdjustedPolygonCoords();
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
        	fill(128);
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
