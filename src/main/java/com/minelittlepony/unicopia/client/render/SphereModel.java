package com.minelittlepony.unicopia.client.render;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.GlAllocationUtils;

public class SphereModel {

    private int displayList;
    private boolean baked;

    protected double posX;
    protected double posY;
    protected double posZ;

    protected float rotX;
    protected float rotY;
    protected float rotZ;

    public void setPosition(double x, double y, double z) {
        posX = x - BlockEntityRenderDispatcher.renderOffsetX;
        posY = y - BlockEntityRenderDispatcher.renderOffsetY;
        posZ = z - BlockEntityRenderDispatcher.renderOffsetZ;
    }

    public void setRotation(float x, float y, float z) {
        rotX = x;
        rotY = y;
        rotZ = z;
    }

    public void render(float scale) {
        if (scale == 0) {
            return;
        }

        if (!baked) {
            baked = true;
            bake();
        }

        GlStateManager.pushMatrix();

        if (posX != 0 && posY != 9 && posZ != 0) {
            GlStateManager.translated(posX, posY, posZ);
        }

        glRotate(rotX, 1, 0, 0);
        glRotate(rotY, 0, 1, 0);
        glRotate(rotZ, 0, 0, 1);

        GlStateManager.scalef(scale, scale, scale);

        GlStateManager.callList(displayList);

        GlStateManager.popMatrix();
    }

    private void bake() {
        displayList = GlAllocationUtils.genLists(1);
        GlStateManager.newList(displayList, GL11.GL_COMPILE);

        drawShape();

        GlStateManager.endList();
    }

    protected void drawShape() {

        /*Sphere sphere = new Sphere();

        sphere.setDrawStyle(GLU.GLU_FILL);
        sphere.setNormals(GLU.GLU_SMOOTH);
        sphere.draw(1, 32, 32);*/
        // TODO: GLU Sphere
    }

    static void glRotate(float angle, float x, float y, float z) {
        if (angle != 0) {
            GlStateManager.rotatef(angle, x, y, z);
        }
    }

}