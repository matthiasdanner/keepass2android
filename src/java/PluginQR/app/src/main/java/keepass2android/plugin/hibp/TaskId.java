package keepass2android.plugin.hibp;

class TaskId {

    private static int currentId = 0;

    private int id;

    TaskId() {
        this.id = currentId++;
    }

    @Override
    public boolean equals(Object obj) {
        TaskId other = (TaskId)obj;
        if (other == null) {
            return false;
        }
        return this.id == other.id;
    }
}
