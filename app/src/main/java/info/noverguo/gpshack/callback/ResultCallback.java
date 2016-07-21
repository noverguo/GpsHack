package info.noverguo.gpshack.callback;

public abstract class ResultCallback<T> implements ErrorCallback {
    @Override
    public void onError(Throwable e) {
    }
    public abstract void onResult(T t);
}