package kinectris32;

import java.util.HashMap;

import netP5.*;
import oscP5.*;
import processing.core.*;

import javax.swing.JOptionPane;

import beads.*;
import SimpleOpenNI.*;


public class Kinectris32 extends PApplet {

	/*public static boolean fullscreen = false;

    static public void main(String args[]) {
        if (fullscreen) {
            PApplet.main(new String[] { "--present", "Kinectris"});
        }
    }
    */

    // debug mode switch
    public static boolean debug = false;
    

    // Kinect
    //SimpleOpenNI kinect;
    boolean autoCalib = true;

    int colT = color(255, 100, 100);
    int colP = color(100, 255, 100);
    Polygon polygonTarget = new Polygon(10, 250, -1500, colT, false, false, false);
    Polygon polygonPlayer = new Polygon(11, 80, -350, colP, true, false, true);
    PFont myFont;
    int scorePosX = 100;
    boolean paused;
    int pauseCounter = 0;
    Star [] stars = new Star[200];

    
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

    public void setup() {
        size(1024, 768, P3D);
        float fov = PI/3.0f;
        float cameraZ = (height/2.0f) / tan(PI*60.0f/360.0f);
        perspective(PI/3.0f, width/height, cameraZ/10.0f, cameraZ*10.0f);
        //perspective(radians(45), width/height, 10f,150000f);
        frameRate(30);
        myFont = createFont("Arial", 20);
        textFont(myFont);
        fill(0);
        hint(ENABLE_NATIVE_FONTS);
        textMode(SCREEN);
        
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
        oscP5.plug(this, "wristLeft", "/skeleton0/WristLeft");
        oscP5.plug(this, "shoulderRight", "kinect/skeleton/0/shoulder_right");
        oscP5.plug(this, "shoulderLeft", "kinect/skeleton/0/shoulder_left");
        oscP5.plug(this, "footRight", "kinect/skeleton/0/foot_right");
        oscP5.plug(this, "footLeft", "kinect/skeleton/0/foot_left");
        oscP5.plug(this, "hipRight", "kinect/skeleton/0/hip_right");
        oscP5.plug(this, "hipLeft", "kinect/skeleton/0/hip_left");
        oscP5.plug(this, "hipCenter", "kinect/skeleton/0/hip_center");
        oscP5.plug(this, "head", "kinect/skeleton/0/head");
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
   }

    
    void oscEvent(OscMessage theOscMessage) {
    	  /* with theOscMessage.isPlugged() you check if the osc message has already been
    	   * forwarded to a plugged method. if theOscMessage.isPlugged()==true, it has already 
    	   * been forwared to another method in your sketch. theOscMessage.isPlugged() can 
    	   * be used for double posting but is not required.
    	  */  
    	  if(theOscMessage.isPlugged()==false) {
    	  /* print the address pattern and the typetag of the received OscMessage */
    	  println("### received an osc message.");
    	  println("### addrpattern\t"+theOscMessage.addrPattern());
    	  println("### typetag\t"+theOscMessage.typetag());
    	  }
    	}

    public void draw() {
        //kinect.update();

        background(0);
        // draw the bg stars
        
        pushMatrix();
        translate(0,0,-1600);
        //drawStars();
        popMatrix();
        fill(255,0,0, 200);

        // draw the game stage
        pushMatrix();
        translate(width/2, height/2, -300);
        fill(255,0,0,0);
        //stroke(0);
        int clr = color(255,0,0);
        drawBox(width * .75f, height * 1f, 1500, -1400, clr);
        //box(width * .75f, height * .75f, 1000);
        popMatrix();

        pushMatrix();
        translate(width/2, height/2, 0);
        strokeWeight(10);
        /*if(kinect.isTrackingSkeleton(1)) {
            println("got a skeleton, drawing lower left arm");
            int userId = 1;
            kinect.drawLimb(userId, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_NECK);

            kinect.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_LEFT_SHOULDER);
            kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_LEFT_ELBOW);
            kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND);

            kinect.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_RIGHT_SHOULDER);
            kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_RIGHT_ELBOW);
            kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, SimpleOpenNI.SKEL_RIGHT_HAND);

            kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
            kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_TORSO);

            kinect.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_LEFT_HIP);
            kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_HIP, SimpleOpenNI.SKEL_LEFT_KNEE);
            kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_KNEE, SimpleOpenNI.SKEL_LEFT_FOOT);

            kinect.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_RIGHT_HIP);
            kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_HIP, SimpleOpenNI.SKEL_RIGHT_KNEE);
            kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, SimpleOpenNI.SKEL_RIGHT_FOOT);
            drawLimb(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND );
        }
        */
        strokeWeight(1);
        popMatrix();

        // draw the game pieces
        if (paused) {
            fill(255,0,0);
            text("Got him", scorePosX, 200, 0);
            pauseCounter++;
            if (pauseCounter > 30) {
                paused = false;
                pauseCounter = 0;
            }
        } else {
            polygonTarget.shootPolygon(polygonTarget.deltaX, polygonTarget.deltaY, polygonTarget.deltaZ);
            polygonTarget.drawPolygon();
            polygonPlayer.drawPolygon();
            // check to see if hit target
            if (polygonTarget.zDepth >= polygonPlayer.zDepth) {
                if (polygonPlayer.checkHit(polygonTarget)) {
                    polygonPlayer.score++;
                    text("HIT!! :-) Score: " + polygonPlayer.score, scorePosX, 100);
                    paused = true;
                } else {
                    text("MISS!! :-( Score: " + polygonPlayer.score, scorePosX, 100);
                }
                polygonTarget.regeneratePolygon();
            }
        }
        //text("Frame Rate: " + frameRate, scorePosX, 20, 0);
        //text("Score: " + polygonPlayer.score, scorePosX, 100, 0);

    }

   /* private void drawLimb(int userId,int jointType1,int jointType2) {
        PVector jointPos1 = new PVector();
        PVector jointPos2 = new PVector();
        float  confidence;

        // draw the joint position
        confidence = kinect.getJointPositionSkeleton(userId,jointType1,jointPos1);
        confidence = kinect.getJointPositionSkeleton(userId,jointType2,jointPos2);

        stroke(255,255,255,confidence * 200 + 55);
        strokeWeight(2);
        pushMatrix();
        translate(0,0,-jointPos1.z);
        ellipse(jointPos1.x,-jointPos1.y, 30, 30);
        ellipse(jointPos2.x,-jointPos2.y, 30, 30);
        popMatrix();
        strokeWeight(1);
        //drawJointOrientation(userId,jointType1,jointPos1,50);
    }
    private void drawJointOrientation(int userId,int jointType,PVector pos,float length)
    {
        // draw the joint orientation
        PMatrix3D  orientation = new PMatrix3D();
        float confidence = kinect.getJointOrientationSkeleton(userId,jointType,orientation);
        if(confidence < 0.001f)
            // nothing to draw, orientation data is useless
            return;

        pushMatrix();
        translate(pos.x,pos.y,pos.z);

        // set the local coordsys
        applyMatrix(orientation);

        // coordsys lines are 100mm long
        // x - r
        stroke(255,0,0,confidence * 200 + 55);
        line(0,0,0,
                length,0,0);
        // y - g
        stroke(0,255,0,confidence * 200 + 55);
        line(0,0,0,
                0,length,0);
        // z - b
        stroke(0,0,255,confidence * 200 + 55);
        line(0,0,0,
                0,0,length);
        popMatrix();
    }
*/
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
	        		break;
	        	}
	        	case 17:{
	        		polygonPlayer.updateJoint(1, x, y, z);
	        		break;
	        	}
	        	case 11:{
	        		polygonPlayer.updateJoint(2, x, y, z);
	        		break;
	        	}
	        	case 9:{
	        		polygonPlayer.updateJoint(3, x, y, z);
	        		break;
	        	}
	        	case 4:{
	        		polygonPlayer.updateJoint(4, x, y, z);
	        		break;
	        	}
	        	case 5:{
	        		polygonPlayer.updateJoint(5, x, y, z);
	        		break;
	        	}
	        	case 7:{
	        		polygonPlayer.updateJoint(6, x, y, z);
	        		break;
	        	}
	        	case 13:{
	        		polygonPlayer.updateJoint(7, x, y, z);
	        		break;
	        	}
	        	case 16: {
	        		polygonPlayer.updateJoint(8, x, y, z);
	        		break;
	        	}
	        	
        	}
        	        	
        	text(jointNumber + " " + name + "= x: " + x + ", y: " + y + ", z: " + z, scorePosX, 100 + (i*20), 0);
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
    public void drawBox(float sizeX, float sizeY, float sizeZ, float offsetZ, int clr) {
        pushMatrix();
        translate(0,0, offsetZ);
        scale(sizeX, sizeY, sizeZ);
        beginShape(QUADS);
        fill(clr, 0);
        stroke(clr);
        vertex(-1,  1,  1);
        vertex( 1,  1,  1);
        vertex( 1, -1,  1);
        vertex(-1, -1,  1);

        vertex( 1,  1,  1);
        vertex( 1,  1, -1);
        vertex( 1, -1, -1);
        vertex( 1, -1,  1);
        fill(clr, 0);

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

    public boolean pointInPolygon(Polygon polyTarget, float[] p) {
        int i = 0;
        float[][] targetPoints =  polyTarget.getPolygon();
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
    
    class Polygon {
        float[][] polygon;
        int clr;
        boolean move, kinect, filled;
        int numPoints;
        float[][] adjustedPolygonCoords;
        float zDepth, ogDepth;
        int polySize;
        float deltaX, deltaY, deltaZ;
        int score, lives;

        Polygon(int n, int size, float z, int c, boolean m, boolean k, boolean f) {
            zDepth = ogDepth = z;
            polySize = size;
            polygon = polygonPathConvexRelative(n, size, z);
            adjustedPolygonCoords = new float[3][n];
            offsetAdjustedPolygonCoords();
            clr = c;
            move = m;
            kinect = k;
            filled = f;
            numPoints = n;
            score = 0;
            lives = 5;
            resetDeltas();
        }

        private void resetDeltas() {
            deltaX = ((float)Math.random() * 6) - 3 ;
            deltaY = ((float)Math.random() * 6) - 3;
            deltaZ = ((float)Math.random() * 10) + 5;
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

        private void regeneratePolygon() {
            polygon = polygonPathConvexRelative(numPoints, polySize, ogDepth);
            resetDeltas();
            offsetAdjustedPolygonCoords();
        }

        private void shootPolygon(float deltaX, float deltaY, float deltaZ) {
            float newZ;
            if (zDepth > 300) {
                zDepth = ogDepth;
                regeneratePolygon();
            } else {
                zDepth += deltaZ;
            }
            for (int i = 0; i < numPoints; i++) {
                polygon[0][i] += deltaX;
                polygon[1][i] += deltaY;
                polygon[2][i] = zDepth;
            }
        }

        private void offsetAdjustedPolygonCoords() {
            float mX = 0;
            float mY = 0;
            float offsetX = width/2;
            float offsetY = height/2;
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
                adjustedPolygonCoords[1][i] = polygon[1][i] + offsetY + mY;
                adjustedPolygonCoords[2][i] = polygon[2][i] + offsetZ;
            }
        }

        private void drawPolygon() {
            // polygon is array of array of coords
            // [[x1, x2, x3, x4...], [y1, y2, y3, y4]]
            int n = polygon[0].length;
            offsetAdjustedPolygonCoords();
            fill(clr, 20);
            stroke(clr, 50);
            pushMatrix();
            translate(width/2, height/2, zDepth);
            //box(width * .75f, height * .75f, 2);
            drawBox(width * .75f, height * 1f, 1f, zDepth, clr);
            popMatrix();
            if (filled) {
                fill(clr);
            } else {
                noFill();
            }
            stroke(clr);
            strokeWeight(10);
            beginShape();
            for (int i=0; i < n; i++) {
                //vertex(adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i], adjustedPolygonCoords[2][i]);
                curveVertex(adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i], adjustedPolygonCoords[2][i]);
                //text(i,adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i] );
            }
            curveVertex(adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0], adjustedPolygonCoords[2][0]);
            //vertex(adjustedPolygonCoords[0][0], adjustedPolygonCoords[1][0], adjustedPolygonCoords[2][0]);
            //text(n-1,adjustedPolygonCoords[0][n-1], adjustedPolygonCoords[1][n-1] );
            endShape(CLOSE);
            strokeWeight(1);
            fill(0);
            if (debug) {
                for (int i=0; i < n; i++) {
                    text(i,adjustedPolygonCoords[0][i], adjustedPolygonCoords[1][i], adjustedPolygonCoords[2][i] );
                }
                text(n-1,adjustedPolygonCoords[0][n-1], adjustedPolygonCoords[1][n-1],adjustedPolygonCoords[2][n-1] );
            }

        }

        private boolean checkHit(Polygon target) {
            int numPlayerPoints = this.numPoints;
            boolean hit = false;
            for (int p = 0; p < numPlayerPoints; p++) {
                float[] point = {this.getPolygon()[0][p], this.getPolygon()[1][p]};
                if (pointInPolygon(target, point)) {
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
            x = (x * width/2);
            y = (-y * height/2);
            polygon[0][joint] = x;
            polygon[1][joint] = y;
            // leave z alone for now
            // polygon[2][joint] = z;
            offsetAdjustedPolygonCoords();
        }
    }
// -----------------------------------------------------------------
// SimpleOpenNI user events
/*
    public void onNewUser(int userId)
    {
        println("onNewUser - userId: " + userId);
        println("  start pose detection");

        if(autoCalib)
            kinect.requestCalibrationSkeleton(userId,true);
        else
            kinect.startPoseDetection("Psi",userId);
    }

    public void onLostUser(int userId)
    {
        println("onLostUser - userId: " + userId);
    }

    public void onStartCalibration(int userId)
    {
        println("onStartCalibration - userId: " + userId);
    }

    public void onEndCalibration(int userId, boolean successfull)
    {
        println("onEndCalibration - userId: " + userId + ", successfull: " + successfull);

        if (successfull)
        {
            println("  User calibrated !!!");
            kinect.startTrackingSkeleton(userId);
        }
        else
        {
            println("  Failed to calibrate user !!!");
            println("  Start pose detection");
            kinect.startPoseDetection("Psi",userId);
        }
    }

    public void onStartPose(String pose,int userId)
    {
        println("onStartdPose - userId: " + userId + ", pose: " + pose);
        println(" stop pose detection");

        kinect.stopPoseDetection(userId);
        kinect.requestCalibrationSkeleton(userId, true);

    }

    public void onEndPose(String pose,int userId)
    {
        println("onEndPose - userId: " + userId + ", pose: " + pose);
    }
	*/
//    static public void main(String args[]) {
//            PApplet.main(new String[] { "--present", "kinectris32.Kinectris32"});
//    }
	public static void main(String _args[]) {
		PApplet.main(new String[] { kinectris32.Kinectris32.class.getName() });
	}
}
