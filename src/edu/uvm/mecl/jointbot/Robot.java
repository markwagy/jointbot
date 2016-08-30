package edu.uvm.mecl.jointbot;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.HingeConstraint;
import edu.uvm.mecl.jointbot.JointBotInfo.Joints;
import javax.vecmath.Vector3f;

/**
 *
 * @author mwagy
 */
public abstract class Robot {
        
    private final float RATE = 100f;
    private final float IMPULSE = 1f;
    public static final float MAX_JOINT_ANGLE = (float) Math.PI * 60f / 180f;
    
    int IDs[];
    
    protected RigidBody bodies[];
    protected HingeConstraint joints[];
    protected CollisionShape[] geoms;
    
    DynamicsWorld world;
    
    public Robot(DynamicsWorld world) {
        this.world = world;
    }
    
    public void setGroundObjectID(RigidBody groundObject) {
        groundObject.setUserPointer(IDs[9]);
    }
    
    /**
     * @return position of main body
     */
    public abstract Vector3f getPosition();
    
    public abstract float getCurrentJointAngle(int jointIndex);

    public void actuateJoint2(int jointIndex, float desiredAngleDegs, float maxImpulse, float rate) {
        float currAngleRads = joints[jointIndex].getHingeAngle();
        float desiredAngleRads = desiredAngleDegs*3.1415f/180;
        float diff = desiredAngleRads-currAngleRads;
        joints[jointIndex].enableAngularMotor(true, rate*diff, maxImpulse);
    }
    
    public void actuateJointSine(JointBotInfo.Joints joint, float t, float phase) {
        actuateJointSine(joint, t, phase, 10f);
    }
    
    public void actuateJointSine(JointBotInfo.Joints joint, float t, float phase, float freq) {
        //float sinval = (float) ((float) (45*Math.PI/180.0) * (Math.sin(t/freq + phase)));
        float currAngle = joints[joint.ordinal()].getHingeAngle();
        float desiredAngle = getSineAngle(t, phase, freq);
        float diff = desiredAngle - currAngle;
        joints[joint.ordinal()].enableAngularMotor(true, RATE*diff, IMPULSE);
    }
    
    public float getSineAngle(float t, float phase, float freq) {
        double sinval = Math.sin(Math.PI*2*t*freq + phase);
        return (float) (MAX_JOINT_ANGLE * sinval);
    }
    
    public void actuateJoint3(JointBotInfo.Joints joint, float t) {
        actuateJointSine(joint, t, 0.0f);
    }
    
    public int getNumberOfJoints() {
        return joints.length;
    }
    
    public void delete() {
        for (int body_idx=0; body_idx<bodies.length; body_idx++) {
            world.removeRigidBody(bodies[body_idx]);
            world.removeCollisionObject(bodies[body_idx]);
        }
        for (int joints_idx=0;joints_idx<joints.length; joints_idx++) {
            world.removeConstraint(joints[joints_idx]);
        }
    }

    public void fixJoint(Joints joint) {
        joints[joint.ordinal()].setLimit(0.0f, 0.0f);
    }
    
    public void unfixJoint(Joints joint) {
        joints[joint.ordinal()].setLimit(0, (float)(45*Math.PI/180));
    }

}
