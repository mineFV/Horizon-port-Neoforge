package org.joml;

/**
 * The stand-in for JOML's Vector3f (MIT, shipped with the game): three floats, and the plain arithmetic of them,
 * each component on its own (observed against JOML, `standins`). Its text, its serialization, and what JOML computes
 * with fused multiply-adds (lengths, normalizing, products) aren't modelled.
 */
public class Vector3f implements java.io.Externalizable, Cloneable, Vector3fc {
    public float x;
    public float y;
    public float z;

    public Vector3f() {}

    public Vector3f(float d) {
        this(d, d, d);
    }

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3f(Vector3fc v) {
        this(v.x(), v.y(), v.z());
    }

    @Override
    public float x() {
        return x;
    }

    @Override
    public float y() {
        return y;
    }

    @Override
    public float z() {
        return z;
    }

    public Vector3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3f set(Vector3fc v) {
        return set(v.x(), v.y(), v.z());
    }

    public Vector3f add(float x, float y, float z) {
        return add(x, y, z, this);
    }

    public Vector3f add(float x, float y, float z, Vector3f dest) {
        return dest.set(this.x + x, this.y + y, this.z + z);
    }

    public Vector3f add(Vector3fc v) {
        return add(v.x(), v.y(), v.z(), this);
    }

    public Vector3f add(Vector3fc v, Vector3f dest) {
        return add(v.x(), v.y(), v.z(), dest);
    }

    public Vector3f sub(float x, float y, float z) {
        return sub(x, y, z, this);
    }

    public Vector3f sub(float x, float y, float z, Vector3f dest) {
        return dest.set(this.x - x, this.y - y, this.z - z);
    }

    public Vector3f sub(Vector3fc v) {
        return sub(v.x(), v.y(), v.z(), this);
    }

    public Vector3f sub(Vector3fc v, Vector3f dest) {
        return sub(v.x(), v.y(), v.z(), dest);
    }

    public Vector3f mul(float scalar) {
        return mul(scalar, scalar, scalar, this);
    }

    public Vector3f mul(float scalar, Vector3f dest) {
        return mul(scalar, scalar, scalar, dest);
    }

    public Vector3f mul(float x, float y, float z) {
        return mul(x, y, z, this);
    }

    public Vector3f mul(float x, float y, float z, Vector3f dest) {
        return dest.set(this.x * x, this.y * y, this.z * z);
    }

    public Vector3f mul(Vector3fc v) {
        return mul(v.x(), v.y(), v.z(), this);
    }

    public Vector3f mul(Vector3fc v, Vector3f dest) {
        return mul(v.x(), v.y(), v.z(), dest);
    }

    public Vector3f mul(Matrix3fc mat) {
        return mul(mat, this);
    }

    /**
     * Observed: the matrix times the vector, each component JOML's fused multiply-adds of its row as JOML runs them by
     * default (joml.useMathFma unset: a product then a sum, innermost first).
     */
    public Vector3f mul(Matrix3fc mat, Vector3f dest) {
        float lx = x, ly = y, lz = z;
        return dest.set(mat.m00() * lx + (mat.m10() * ly + mat.m20() * lz), mat.m01() * lx + (mat.m11() * ly + mat.m21() * lz),
                mat.m02() * lx + (mat.m12() * ly + mat.m22() * lz));
    }

    public Vector3f cross(Vector3fc v) {
        return cross(v.x(), v.y(), v.z(), this);
    }

    public Vector3f cross(Vector3fc v, Vector3f dest) {
        return cross(v.x(), v.y(), v.z(), dest);
    }

    public Vector3f cross(float x, float y, float z) {
        return cross(x, y, z, this);
    }

    /** Observed: each component JOML's multiply-add as it runs it by default (a product, then the other's negated product added). */
    public Vector3f cross(float x, float y, float z, Vector3f dest) {
        float rx = this.y * z + -this.z * y;
        float ry = this.z * x + -this.x * z;
        float rz = this.x * y + -this.y * x;
        return dest.set(rx, ry, rz);
    }

    public Vector3f normalize() {
        return normalize(this);
    }

    /**
     * Observed: each component times one over the square root of the sum of squares, that sum JOML's multiply-adds as
     * it runs them by default (innermost first).
     */
    public Vector3f normalize(Vector3f dest) {
        float scalar = 1.0F / (float) Math.sqrt(x * x + (y * y + z * z));
        return dest.set(x * scalar, y * scalar, z * scalar);
    }

    public Vector3f negate() {
        return set(-x, -y, -z);
    }

    /** Observed: each component's bits, as Float.floatToIntBits gives them, folded by 31 from 1. */
    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(x);
        result = 31 * result + Float.floatToIntBits(y);
        result = 31 * result + Float.floatToIntBits(z);
        return result;
    }

    /** Observed: a Vector3f (of that very class) whose components have the same bits. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vector3f other = (Vector3f) obj;
        return Float.floatToIntBits(x) == Float.floatToIntBits(other.x) && Float.floatToIntBits(y) == Float.floatToIntBits(other.y)
                && Float.floatToIntBits(z) == Float.floatToIntBits(other.z);
    }

    /** JOML writes it with a NumberFormat: not modelled. */
    @Override
    public String toString() {
        throw new UnsupportedOperationException("a Vector3f's text isn't modelled (horizon-port-neoforge)");
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public void writeExternal(java.io.ObjectOutput out) {
        throw new UnsupportedOperationException("serializing a Vector3f isn't modelled (horizon-port-neoforge)");
    }

    @Override
    public void readExternal(java.io.ObjectInput in) {
        throw new UnsupportedOperationException("serializing a Vector3f isn't modelled (horizon-port-neoforge)");
    }
}
