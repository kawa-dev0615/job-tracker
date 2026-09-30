package jobtracker.repository;

/** データの読み書きに失敗したときの例外 */
public class RepositoryException extends RuntimeException {

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
