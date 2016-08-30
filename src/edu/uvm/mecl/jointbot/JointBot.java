package edu.uvm.mecl.jointbot;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.collision.shapes.CylinderShapeX;
import com.bulletphysics.collision.shapes.CylinderShapeZ;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.HingeConstraint;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import edu.uvm.mecl.jointbot.JointBotInfo.Bodies;
import edu.uvm.mecl.jointbot.JointBotInfo.Joints;
import javax.vecmath.Vector3f;


/**
 *
 * @author mwagy
 */
public class JointBot extends Robot {

    private final float CYL_LENGTH = 8f;
    private final float CYL_RADIUS = 1f;

    public enum Direction {X,Y,Z};
    
//    final float JOINT_RANGE_ANGLE = 45*3.1415f/180;
    
    Vector3f pivotA;
    Vector3f pivotB;
    Vector3f axisA;
    Vector3f axisB;
    Vector3f axis;
    
    public JointBot(DynamicsWorld world) {
        super(world);
        joints = new HingeConstraint[Joints.Count.ordinal()];
        bodies = new RigidBody[Bodies.Count.ordinal()];
        geoms =  new CollisionShape[Bodies.Count.ordinal()];
        IDs = new int[Bodies.Count.ordinal() + 1]; // plus one for ground
        
        for (int i=0; i<IDs.length; i++) {
            IDs[i] = i;
        }
        setUpBodies();
        setUpJoints();
    }
    
    private void setUpBodies() {
        
        // spine
        geoms[Bodies.Spine1.ordinal()] = 
                createCylinderX(Bodies.Spine1.ordinal(),
                CYL_LENGTH/2,0f,0f);
        geoms[Bodies.Spine2.ordinal()] =
                createCylinderX(Bodies.Spine2.ordinal(),
                -CYL_LENGTH/2,0f,0f);

        // leg 1
        geoms[Bodies.UpperLeg1.ordinal()] =
                createCylinderZ(Bodies.UpperLeg1.ordinal(),
                CYL_LENGTH,0f,-CYL_LENGTH/2);

        geoms[Bodies.LowerLeg1.ordinal()] =
                createCylinderZ(Bodies.LowerLeg1.ordinal(),
                CYL_LENGTH,0f,-CYL_LENGTH/2-CYL_LENGTH);

        // leg 2
        geoms[Bodies.UpperLeg2.ordinal()] =
                createCylinderZ(Bodies.UpperLeg2.ordinal(),
                CYL_LENGTH,0f,CYL_LENGTH/2);
        geoms[Bodies.LowerLeg2.ordinal()] =
                createCylinderZ(Bodies.LowerLeg2.ordinal(),
                CYL_LENGTH,0f,CYL_LENGTH/2+CYL_LENGTH);

        // leg 3
        geoms[Bodies.UpperLeg3.ordinal()] =
                createCylinderZ(Bodies.UpperLeg3.ordinal(),
                -CYL_LENGTH,0f,CYL_LENGTH/2);

        geoms[Bodies.LowerLeg3.ordinal()] =
                createCylinderZ(Bodies.LowerLeg3.ordinal(),
                -CYL_LENGTH,0f,CYL_LENGTH/2+CYL_LENGTH);

        // leg 4
        geoms[Bodies.UpperLeg4.ordinal()] =
                createCylinderZ(Bodies.UpperLeg4.ordinal(),
                -CYL_LENGTH,0f,-CYL_LENGTH/2);

        geoms[Bodies.LowerLeg4.ordinal()] =
                createCylinderZ(Bodies.LowerLeg4.ordinal(),
                -CYL_LENGTH,0f,-CYL_LENGTH/2-CYL_LENGTH);

    }
    
    private void setUpJoints() {
        // spine 1 - spine 2
        pivotA = new Vector3f(-CYL_LENGTH/2,0f,0f);
        pivotB = new Vector3f(CYL_LENGTH/2,0f,0f);
        axisA = new Vector3f(0f,0f,-1f);
        axisB = new Vector3f(0f,0f,-1f);
        createHinge(Joints.Spine1_Spine2.ordinal(),
                bodies[Bodies.Spine1.ordinal()], bodies[Bodies.Spine2.ordinal()],
                pivotA, pivotB, axisA, axisB);

        // leg axes are all the same
        axisA = new Vector3f(-1f,0f,0f);
        axisB = new Vector3f(-1f,0f,0f);
        
        // shoulder axis
        Vector3f axisShoulder = new Vector3f(0f,0f,-1f);

        // spine1 - leg 1
        axis = new Vector3f(1f,0f,0f);
        pivotA = new Vector3f(CYL_LENGTH/2,0f,0f);
        pivotB = new Vector3f(0f,0f,CYL_LENGTH/2);
        createHinge(Joints.Spine1_UpperLeg1.ordinal(),
                bodies[Bodies.Spine1.ordinal()], bodies[Bodies.UpperLeg1.ordinal()],
//                pivotA, pivotB, axis, axis);
                pivotA, pivotB, axisShoulder, axisShoulder);

        // spine1 leg 2
        pivotA = new Vector3f(CYL_LENGTH/2,0f,0f);
        pivotB = new Vector3f(0f,0f,-CYL_LENGTH/2);
        createHinge(Joints.Spine1_UpperLeg2.ordinal(),
                bodies[Bodies.Spine1.ordinal()], bodies[Bodies.UpperLeg2.ordinal()],
//                pivotA, pivotB, axisA, axisB);
                        pivotA, pivotB, axisShoulder, axisShoulder);

        // spine2 - leg 3
        pivotA = new Vector3f(-CYL_LENGTH/2,0f,0f);
        pivotB = new Vector3f(0f,0f,-CYL_LENGTH/2);
        createHinge(Joints.Spine2_UpperLeg3.ordinal(),
                bodies[Bodies.Spine2.ordinal()], bodies[Bodies.UpperLeg3.ordinal()],
//                pivotA, pivotB, axisA, axisB);
                        pivotA, pivotB, axisShoulder, axisShoulder);

        // spine2 - leg 4
        pivotA = new Vector3f(-CYL_LENGTH/2,0f,0f);
        pivotB = new Vector3f(0f,0f,CYL_LENGTH/2);
        createHinge(Joints.Spine2_UpperLeg4.ordinal(),
                bodies[Bodies.Spine2.ordinal()], bodies[Bodies.UpperLeg4.ordinal()],
//                pivotA, pivotB, axis, axis);
                        pivotA, pivotB, axisShoulder, axisShoulder);

        // leg 1
        pivotA = new Vector3f(0f,0f,-CYL_LENGTH/2);
        pivotB = new Vector3f(0f,0f,CYL_LENGTH/2);
        createHinge(Joints.UpperLeg1_LowerLeg1.ordinal(),
                bodies[Bodies.UpperLeg1.ordinal()], bodies[Bodies.LowerLeg1.ordinal()],
                pivotA, pivotB, axis, axis);
        
        // leg 2
        pivotA = new Vector3f(0f,0f,CYL_LENGTH/2);
        pivotB = new Vector3f(0f,0f,-CYL_LENGTH/2);
        createHinge(Joints.UpperLeg2_LowerLeg2.ordinal(),
                bodies[Bodies.UpperLeg2.ordinal()], bodies[Bodies.LowerLeg2.ordinal()],
                pivotA, pivotB, axisA, axisB);

        // leg 3 
        pivotA = new Vector3f(0f,0f,CYL_LENGTH/2);
        pivotB = new Vector3f(0f,0f,-CYL_LENGTH/2);
        createHinge(Joints.UpperLeg3_LowerLeg3.ordinal(),
                bodies[Bodies.UpperLeg3.ordinal()], bodies[Bodies.LowerLeg3.ordinal()],
                pivotA, pivotB, axisA, axisB);

        // leg 4
        pivotA = new Vector3f(0f,0f,-CYL_LENGTH/2);
        pivotB = new Vector3f(0f,0f,CYL_LENGTH/2);
        createHinge(Joints.UpperLeg4_LowerLeg4.ordinal(),
                bodies[Bodies.UpperLeg4.ordinal()], bodies[Bodies.LowerLeg4.ordinal()],
                pivotA, pivotB, axis, axis);
    }
            
    public void actuateJoint(
            int jointIndex, float desiredAngle, float jointOffset, float timeStep) {
        float targetVelocity = 0.1f;
        float maxMotorImpulse = 0.1f;
        joints[jointIndex].enableAngularMotor(true, targetVelocity, maxMotorImpulse);
    }
    
    
    /* get joint angle in degrees */
    public float getCurrentJointAngle(int jointIdx) {
        return joints[jointIdx].getHingeAngle()*180.0f/3.1415f;
    }
    
    private CollisionShape createCylinderX(int index, float x, float y, float z) {
        CylinderShape cyl;
        cyl = new CylinderShapeX(
                new Vector3f(CYL_LENGTH/2,CYL_RADIUS,CYL_RADIUS));
        Transform trans = new Transform();
        trans.setIdentity();
        trans.origin.set(x, y, z);
        RigidBody body = createRigidBody(trans, cyl);
        bodies[index] = body;
        bodies[index].setUserPointer(IDs[index]);
        world.addRigidBody(body);
        trans.setIdentity();
        return cyl;
    }
    
    private CollisionShape createCylinderZ(int index, float x, float y, float z) {
        //System.out.println("Creating cylinder with user pointer: " + index);
        CylinderShape cyl = new CylinderShapeZ(
                new Vector3f(CYL_RADIUS, CYL_RADIUS, CYL_LENGTH/2));
        Transform trans = new Transform();
        trans.setIdentity();
        trans.origin.set(x, y, z);
        RigidBody body = createRigidBody(trans, cyl);
        bodies[index] = body;
        bodies[index].setUserPointer(IDs[index]);
        world.addRigidBody(body);
        return cyl;
    }
    
    private RigidBody createRigidBody(Transform startTransform, CollisionShape shape) {
        float mass = 1.0f;
        Vector3f localInertia = new Vector3f();
        localInertia.set(0f,0f,0f);
        shape.calculateLocalInertia(mass, localInertia);
        DefaultMotionState myMotionState = 
                new DefaultMotionState(startTransform);
        RigidBodyConstructionInfo rbInfo = 
                new RigidBodyConstructionInfo(mass, myMotionState, shape, localInertia);
        rbInfo.additionalDamping = true;
        RigidBody body = new RigidBody(rbInfo);
        return body;
    }
    
    private void createHinge(int index,
            RigidBody rbA, RigidBody rbB, 
            Vector3f pivotA_local, Vector3f pivotB_local,
            Vector3f axisA_local, Vector3f axisB_local) {
        HingeConstraint hc = new HingeConstraint(
                rbA, rbB,
                pivotA_local, pivotB_local,
                axisA_local, axisB_local);
//        hc.setLimit(-JOINT_RANGE_ANGLE, JOINT_RANGE_ANGLE);
        hc.setLimit(0, Robot.MAX_JOINT_ANGLE);
        joints[index] = hc;
        world.addConstraint(hc, true);
    }
    
    /**
     * @return position of main body
     */
    @Override
    public Vector3f getPosition() {
        Vector3f pos = new Vector3f();
        bodies[Bodies.Spine1.ordinal()].getCenterOfMassPosition(pos);
        pos.x-=CYL_LENGTH/2;
        return pos;
    }
    
    public Vector3f[] getBodyEndpoints(Bodies bodyType) {
        Vector3f pos1 = new Vector3f();
        Vector3f pos2 = new Vector3f();
        Vector3f com = new Vector3f();
        bodies[bodyType.ordinal()].getCenterOfMassPosition(com);
        if (bodyType.equals(Bodies.Spine1) || bodyType.equals(Bodies.Spine2)) {
            pos1.x = com.x - CYL_LENGTH/2;
            pos1.y = com.x;
            pos1.z = com.z;
            pos2.x = com.x + CYL_LENGTH/2;
            pos2.y = com.y;
            pos2.z = com.z;
        } else {
            pos1.x = com.x;
            pos1.y = com.y - CYL_LENGTH/2;
            pos1.z = com.z;
            pos2.x = com.x;
            pos2.y = com.y + CYL_LENGTH/2;
            pos2.z = com.z;
        }
        Vector3f[] endPoints = new Vector3f[2];
        endPoints[0] = pos1;
        endPoints[1] = pos2;
        return endPoints;
    }
    
    public Vector3f[] getAABB(Bodies bodyType) {
        Vector3f pos1 = new Vector3f();
        Vector3f pos2 = new Vector3f();
        Vector3f pos[] = new Vector3f[2];
        bodies[bodyType.ordinal()].getAabb(pos1, pos2);
        pos[0] = pos1;
        pos[1] = pos2;
        return pos;
    }
}
