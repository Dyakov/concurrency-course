package course.concurrency.exams.refactoring;

public class MountainTableManagerFactory {

    public Manager createManager(String address) {
        return new Others.MountTableManager(address);
    }
}
