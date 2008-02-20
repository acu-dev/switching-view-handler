package com.sun.facelets;

import java.io.IOException;

import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;
import javax.portlet.PortletContext;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

public class FaceletSwitchingViewHandler extends FaceletViewHandler {
	private static final Logger logger = Logger.getLogger(FaceletSwitchingViewHandler.class);
	private int bufferSize;

	public FaceletSwitchingViewHandler(ViewHandler parent) {
		super(parent);
	}

	protected ResponseWriter createResponseWriter(FacesContext context)
			throws IOException, FacesException {
		ExternalContext extContext = context.getExternalContext();
		RenderKit renderKit = context.getRenderKit();

		logger.debug(String.format("extContext.getContext.class: %s", extContext.getContext().getClass().getName()));
		
		if (extContext.getContext() instanceof PortletContext) {
			RenderRequest request = (RenderRequest) extContext.getRequest();
			RenderResponse response = (RenderResponse) extContext.getResponse();

			String contenttype = request.getResponseContentType();
			if (contenttype == null) {
				contenttype = "text/html";
			}

			String encoding = response.getCharacterEncoding();
			if (encoding == null) {
				encoding = "ISO-8859-1";
			}

			ResponseWriter writer = renderKit.createResponseWriter(
					NullWriter.Instance, contenttype, encoding);

			contenttype = writer.getContentType();

			response.setContentType(contenttype);

			writer = writer.cloneWithWriter(response.getWriter());

			return writer;
		} else {
			if (renderKit == null) {
				String id = context.getViewRoot().getRenderKitId();
				throw new IllegalStateException(
						"No render kit was available for id \"" + id + "\"");
			}

			ServletResponse response = (ServletResponse) extContext
					.getResponse();

			if (this.bufferSize != -1) {
				response.setBufferSize(this.bufferSize);
			}

			String contentType = (String) extContext.getRequestMap().get(
					"facelets.ContentType");

			String encoding = (String) extContext.getRequestMap().get(
					"facelets.Encoding");

			ResponseWriter writer;
			if (contentType != null && !contentType.equals("*/*")) {
				contentType += ",*/*";
			}
			try {
				writer = renderKit.createResponseWriter(NullWriter.Instance,
						contentType, encoding);
			} catch (IllegalArgumentException e) {
				writer = renderKit.createResponseWriter(NullWriter.Instance,
						"*/*", encoding);
			}

			contentType = getResponseContentType(context, writer
					.getContentType());
			encoding = getResponseEncoding(context, writer
					.getCharacterEncoding());

			response.setContentType(contentType + "; charset=" + encoding);

			writer = writer.cloneWithWriter(response.getWriter());

			return writer;
		}
	}
}