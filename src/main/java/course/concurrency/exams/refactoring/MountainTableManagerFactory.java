package course.concurrency.exams.refactoring;

public class MountainTableManagerFactory {

    public Others.MountTableManager createManager(String address) {
        return new Others.MountTableManager(address);
    }
}
