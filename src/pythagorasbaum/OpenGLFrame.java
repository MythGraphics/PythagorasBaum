/*
 *
 */

package pythagorasbaum;

/**
 *
 * @author  Martin Pröhl alias MythGraphics
 * @version 1.0.0
 *
 */

import java.nio.IntBuffer;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.system.MemoryUtil.NULL;

public class OpenGLFrame {

	private long window;                                                                                                // The window handle
    private final int winw = 640, winh = 480;                                                                           // Window width, Window height

    public static void main(String[] args) {
        new OpenGLFrame().run();
    }

	public void run() {
		init();
		loop();
		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);
		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private void init() {
		// Setup an error callback.
        GLFWErrorCallback.createPrint(System.err).set();
		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if ( !glfwInit() ) {
			throw new IllegalStateException("Unable to initialize GLFW");
        }
		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
		// Create the window
		window = glfwCreateWindow(winw, winh, "Quadrat", NULL, NULL);
		if ( window == NULL ) {
            glfwTerminate();
			throw new RuntimeException("Failed to create the GLFW window");
        }
		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE ) {
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            }
		});
        // Get the resolution of the primary monitor
        GLFWVidMode vidmode = glfwGetVideoMode( glfwGetPrimaryMonitor() );
        // Center the window on screen
        glfwSetWindowPos(
            window,
            (vidmode.width() - winw) / 2,
            (vidmode.height() - winh) / 2
        );
		// Make the OpenGLFrame context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);
        // Make the window visible
		glfwShowWindow(window);
	}

    private void render(IntBuffer[] buffers) {
        float ratio;
        IntBuffer width  = buffers[0];
        IntBuffer height = buffers[1];

        /* Get width and height to calcualte the ratio */
        glfwGetFramebufferSize(window, width, height);
        ratio = width.get() / (float) height.get();

        /* Rewind buffers for next get */
        width.rewind();
        height.rewind();

        /* Set viewport */
        glViewport(0, 0, width.get(), height.get());

        /* Set ortographic projection */
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();                                                                                               // ersetzt die aktuelle Matrix durch die Einheitsmatrix (identity matrix)
        glOrtho(-ratio, ratio, -1f, 1f, -1f, 1f);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();                                                                                               // ersetzt die aktuelle Matrix durch die Einheitsmatrix (identity matrix)

        /* Rotate matrix */
        glRotatef((float) glfwGetTime()*50f, 0f, 0f, 1f); // Rotation um die Z-Achse

        /* Render square */
        glBegin(GL11.GL_QUADS);
        glColor3f(1f, 0f, 0f);
        glVertex2f(-0.5f, -0.5f);
        glColor3f(0f, 1f, 0f);
        glVertex2f(0.5f, -0.5f);
        glColor3f(0f, 0f, 1f);
        glVertex2f(0.5f, 0.5f);
        glColor3f(1f, 1f, 1f);
        glVertex2f(-0.5f, 0.5f);
        glEnd();

        /* Flip buffers for next loop */
        width.flip();
        height.flip();
    }

	private void loop() {
		/* This line is critical for LWJGL's interoperation with GLFW's
		   OpenGLFrame context, or any context that is managed externally.
		   LWJGL detects the context that is current in the current thread,
		   creates the GLCapabilities instance and makes the OpenGLFrame
		   bindings available for use.
           Will let lwjgl know we want to use this context as the context to draw with */
		GL.createCapabilities();
        // Set the clear color
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        // enable anti-aliasing, but doesn't seem to work
//      glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);
        // Declare buffers for using inside the loop
        IntBuffer width = MemoryUtil.memAllocInt(1);
        IntBuffer height = MemoryUtil.memAllocInt(1);
        /* Run the rendering loop until the user has attempted to close
		   the window or has pressed the ESCAPE key. */
		while ( !glfwWindowShouldClose(window) ) {
            // clear the framebuffer
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            // do the rendering work
            render(new IntBuffer[] {width, height});
            // swap the framebuffer
			glfwSwapBuffers(window);
			/* Poll for window events.
               The key callback above will only be invoked during this call. */
			glfwPollEvents();
		}
        // Free buffers
        MemoryUtil.memFree(width);
        MemoryUtil.memFree(height);
	}

}
