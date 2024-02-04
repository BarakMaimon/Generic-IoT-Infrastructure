import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/teFst")  // Specify the URL pattern for this servlet
public class TestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");  // Set the content type to HTML
        
        // Get the PrintWriter object to write the HTML response
        PrintWriter out = response.getWriter();

        // Write your HTML content here
        out.println("<html>");
        out.println("<head><title>Test Servlet</title></head>");
        out.println("<body>");
        out.println("<h1>Hello, this is a test servlet!</h1>");
        out.println("</body>");
        out.println("</html>");

        // Close the PrintWriter
        out.close();

    }
}


