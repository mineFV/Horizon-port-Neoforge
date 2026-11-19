package org.joml;

/**
 * The stand-in for JOML's Matrix3f (MIT, shipped with the game): its elements, the identity first, equality and hash
 * (observed against JOML, `standins`). Its operations, its text and its serialization aren't modelled.
 */
public class Matrix3f implements java.io.Externalizable, Cloneable, Matrix3fc {
    public float m00;
    public float m01;
    public float m02;
    public float m10;
    public float m11;
    public float m12;
    public float m20;
    public float m21;
    public float m22;

    public Matrix3f() {
        m00 = 1.0F;
        m11 = 1.0F;
        m22 = 1.0F;
    }

    public Matrix3f(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }

    public Matrix3f(Matrix3fc m) {
        this(m.m00(), m.m01(), m.m02(), m.m10(), m.m11(), m.m12(), m.m20(), m.m21(), m.m22());
    }

    @Override
    public float m00() {
        return m00;
    }

    @Override
    public float m01() {
        return m01;
    }

    @Override
    public float m02() {
        return m02;
    }

    @Override
    public float m10() {
        return m10;
    }

    @Override
    public float m11() {
        return m11;
    }

    @Override
    public float m12() {
        return m12;
    }

    @Override
    public float m20() {
        return m20;
    }

    @Override
    public float m21() {
        return m21;
    }

    @Override
    public float m22() {
        return m22;
    }

    /** Observed: each element's bits, row by row of columns, folded by 31 from 1. */
    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(m00);
        result = 31 * result + Float.floatToIntBits(m01);
        result = 31 * result + Float.floatToIntBits(m02);
        result = 31 * result + Float.floatToIntBits(m10);
        result = 31 * result + Float.floatToIntBits(m11);
        result = 31 * result + Float.floatToIntBits(m12);
        result = 31 * result + Float.floatToIntBits(m20);
        result = 31 * result + Float.floatToIntBits(m21);
        result = 31 * result + Float.floatToIntBits(m22);
        return result;
    }

    /** Observed: a Matrix3f (of that very class) whose elements have the same bits. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Matrix3f other = (Matrix3f) obj;
        return Float.floatToIntBits(m00) == Float.floatToIntBits(other.m00) && Float.floatToIntBits(m01) == Float.floatToIntBits(other.m01) && Float.floatToIntBits(m02) == Float.floatToIntBits(other.m02) && Float.floatToIntBits(m10) == Float.floatToIntBits(other.m10) && Float.floatToIntBits(m11) == Float.floatToIntBits(other.m11) && Float.floatToIntBits(m12) == Float.floatToIntBits(other.m12) && Float.floatToIntBits(m20) == Float.floatToIntBits(other.m20) && Float.floatToIntBits(m21) == Float.floatToIntBits(other.m21) && Float.floatToIntBits(m22) == Float.floatToIntBits(other.m22);
    }

    /** JOML writes it with a NumberFormat: not modelled. */
    @Override
    public String toString() {
        throw new UnsupportedOperationException("a Matrix3f's text isn't modelled (horizon-port-neoforge)");
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public void writeExternal(java.io.ObjectOutput out) {
        throw new UnsupportedOperationException("serializing a Matrix3f isn't modelled (horizon-port-neoforge)");
    }

    @Override
    public void readExternal(java.io.ObjectInput in) {
        throw new UnsupportedOperationException("serializing a Matrix3f isn't modelled (horizon-port-neoforge)");
    }
}
