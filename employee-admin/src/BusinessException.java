/**
 * 业务校验失败。与程序缺陷（如 NPE）区分开：这是"用户输入不合法"的预期错误，
 * 调用方应当捕获并给出友好提示，而不是让程序崩溃。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}