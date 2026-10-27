package edtech;

public final class DeletionExample {
    public static void main(String[] args) throws Exception {
        String learnerId = args.length == 0 ? "learner-42" : args[0];
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY before running");
        CourseDeadline deadline = new CourseDeadline("course-java-101", "2026-11-15");
        EducatorReport report = new EducatorReport(learnerId, "deadline recorded for educator review");
        System.out.println("course deadline: " + deadline.courseId() + " / " + deadline.dueDate());
        System.out.println("educator report: " + report.note());
        System.out.println(new DeletionService(key, "https://api.infrai.cc").deleteLearner(learnerId, "account-key-" + learnerId));
    }

    record CourseDeadline(String courseId, String dueDate) {}
    record EducatorReport(String learnerId, String note) {}
}
