package course.concurrency.m3_shared.practice;

public class CollisionKey {

    private int value;

    public CollisionKey(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollisionKey)) return false;
        CollisionKey that = (CollisionKey) o;
        return value == that.value;
    }

    /*
     * в зависимости от value будет возвращать чётные,
     * либо не чётные значения,
     * чтобы получить коллизии и
     * ускорить вероятность возникновения deadlock
     * */
    @Override
    public int hashCode() {
        return value % 2;
    }

    @Override
    public String toString() {
        return value + "";
    }
}
