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

@WebServlet("/Company")
public class Company extends HttpServlet {

	private static final long serialVersionUID = 3L;
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
		try {

			String[] fields = { "company_name", "company_address", "contact_name", "contact_phone", "contact_email",
					"service_fee", "card_holder_name", "card_number", "ex_date", "CVV" };

			JsonObject startLine = WebsiteContextListener.createJsonObject("POST", "/Company");

			// Create the main JSON object
			JsonObject data = WebsiteContextListener.HttpRecToJson(startLine, req, fields);

			if (!WebsiteContextListener.isValid(data, fields)) {
				extralHtml = "<p style=\"color: red;\">Company registration failed. missing values.</p>";
			}
			Map<String, Object> columnValues = Map.of("company_name", data.getString("company_name"));
			if (adminDBManager.areEntitiesRegistered("Companies", columnValues)) {
				extralHtml = "<p style=\"color: red;\">Company registration failed. Company already register.</p>";
			}

			if (extralHtml == null) {
				JsonObject jsonObject = adminDBManager.registerComapny(data);
				if (jsonObject.getInt("status_code") == 500) {
					req.setAttribute("status_code", 500);
				} else {
					status_code = WebsiteContextListener.sendToGateway(data);

					// Set status message attribute
					req.setAttribute("status_code", status_code);
				}

				extralHtml = (status_code == 400)
						? "<p style=\"color: red;\">Company registration failed. Please try again.</p>"
						: "<p style=\"color: green;\">Company registered successfully!</p>";
			}
			
			resp.setContentType("text/html");
			String htmlFilePath = getServletContext().getRealPath("/index.html"); // TODO

			// Send the HTML content with the added paragraph
			resp.getWriter().write(WebsiteContextListener.getIndexHtml(htmlFilePath, extralHtml));

		} catch (Exception e) {
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
		}
	}

}
