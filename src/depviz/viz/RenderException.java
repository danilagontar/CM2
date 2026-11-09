package depviz.viz;

/**
 * Ошибка при сохранении описания графа или построении изображения.
 */
public class RenderException extends Exception {

    private static final long serialVersionUID = 1L;

    public RenderException(String message) {
        super(message);
    }
}
