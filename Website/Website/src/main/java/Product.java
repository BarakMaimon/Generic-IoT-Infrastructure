import java.io.IOException;
import java.util.Map;

import javax.json.JsonObject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/Product")
public class Product extends HttpServlet {
	private static final long serialVersionUID = 2L;
	private AdminDBManager adminDBManager;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext context = config.getServletContext();
		adminDBManager = (AdminDBManager) context.getAttribute("adminDBManager");

		if (adminDBManager == null) {
			throw new ServletException("adminDBManager not found in servlet context.");
		}

	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String extralHtml = null;
		int status_code = 400;

		String[] fields = { "company_name", "product_name", "product_description" };
		JsonObject startLine = WebsiteContextListener.createJsonObject("POST", "/Product");

		// Create the main JSON object
		JsonObject data = WebsiteContextListener.HttpRecToJson(startLine, req, fields);

		if (!WebsiteContextListener.isValid(data, fields)) {
			extralHtml = "<p style=\"color: red;\">Product registration failed. missing values.</p>";
		}
		Map<String, Object> columnValues = Map.of("company_name", data.getString("company_name"), "product_name",
				data.getString("product_name"));

		if (adminDBManager.areEntitiesRegistered("Products", columnValues)) {
			extralHtml = "<p style=\"color: red;\">Product registration failed. Product already register.</p>";
		}

		if (extralHtml == null) {
			JsonObject jsonObject = adminDBManager.registerProduct(data);
			if (jsonObject.getInt("status_code") == 400) {
				req.setAttribute("status_code", 500);
			} else {
				status_code = WebsiteContextListener.sendToGateway(data);

				// Set status message attribute
				req.setAttribute("status_code", status_code);
			}
			extralHtml = (status_code == 400)
					? "<p style=\"color: red;\">Product registration failed. Please try again.</p>"
					: "<p style=\"color: green;\">Product registered successfully!</p>";
		}

		resp.setContentType("text/html");
		String htmlFilePath = getServletContext().getRealPath("/index.html");// TODO
		resp.getWriter().write(WebsiteContextListener.getIndexHtml(htmlFilePath, extralHtml));

	}

}