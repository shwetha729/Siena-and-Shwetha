/**
 * Reports are Freeware Code Snippets
 *
 * This report is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package tree.output;

import tree.IndiBox;
import tree.Translator;
import tree.graphics.FooterRenderer;
import tree.graphics.GraphicsRenderer;
import tree.graphics.TitleRenderer;

/**
 * Creates classes that render the tree to a graphics object.
 *
 * @author Przemek Wiech <pwiech@losthive.org>
 */
public class RendererFactory {
    public TreeRendererBase renderer;
    public GraphicsRenderer rotateRenderer;
    public TitleRenderer titleRenderer;
    public FooterRenderer footerRenderer;

    public RendererFactory(Translator translator)
    {
        renderer = new VerticalTreeRenderer();
        rotateRenderer = new RotateRenderer(renderer);
        titleRenderer = new TitleRenderer(rotateRenderer);
        footerRenderer = new FooterRenderer(titleRenderer, translator);
    }

    public GraphicsRenderer createRenderer(IndiBox firstIndi, TreeElements elements)
    {
        renderer.setElements(elements);
        renderer.setFirstIndi(firstIndi);
        footerRenderer.setFirstIndi(firstIndi);
        titleRenderer.setIndi(firstIndi);

        return footerRenderer;
    }

}
