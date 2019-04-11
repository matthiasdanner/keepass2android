package keepass2android.plugin.hibp;

interface HIBPClientDelegate {

    void finishedPasswordCheck(boolean requestSuccess, boolean passwordSave);
}
