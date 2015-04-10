package com.vass.portlet.journalcontent.action;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.struts.BaseStrutsPortletAction;
import com.liferay.portal.kernel.struts.StrutsPortletAction;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalArticleDisplay;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journalcontent.util.JournalContentUtil;
import com.liferay.util.portlet.PortletRequestUtil;

import javax.portlet.PortletConfig;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;

public class CustomJournalContentViewAction extends BaseStrutsPortletAction {

	@Override
	public String render(StrutsPortletAction originalStrutsPortletAction,
			PortletConfig portletConfig, RenderRequest renderRequest,
			RenderResponse renderResponse) throws Exception {

		PortletPreferences portletPreferences = renderRequest.getPreferences();

		ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest
				.getAttribute(WebKeys.THEME_DISPLAY);

		long articleGroupId = ParamUtil
				.getLong(renderRequest, "articleGroupId");

		if (articleGroupId <= 0) {
			articleGroupId = GetterUtil.getLong(portletPreferences.getValue(
					"groupId", String.valueOf(themeDisplay.getScopeGroupId())));
		}

		String articleId = ParamUtil.getString(renderRequest, "articleId");
		String ddmTemplateKey = ParamUtil.getString(renderRequest,
				"ddmTemplateKey");

		if (Validator.isNull(articleId)) {
			articleId = GetterUtil.getString(portletPreferences.getValue(
					"articleId", null));
			ddmTemplateKey = GetterUtil.getString(portletPreferences.getValue(
					"ddmTemplateKey", null));
		}

		String viewMode = ParamUtil.getString(renderRequest, "viewMode");
		String languageId = LanguageUtil.getLanguageId(renderRequest);
		int page = ParamUtil.getInteger(renderRequest, "page", 1);
		String xmlRequest = PortletRequestUtil.toXML(renderRequest,
				renderResponse);

		JournalArticle article = null;
		JournalArticleDisplay articleDisplay = null;

		HttpServletRequest httpRequest = PortalUtil
				.getOriginalServletRequest(PortalUtil
						.getHttpServletRequest(renderRequest));

		boolean preview = ParamUtil.get(httpRequest, "preview", false);

		PermissionChecker permissionChecker = themeDisplay
				.getPermissionChecker();

		if (_log.isDebugEnabled()) {
			_log.debug("preview " + preview);
		}

		boolean hasPermission = permissionChecker.hasPermission(articleGroupId,
				"com.liferay.portlet.journal.model.JournalArticle",
				articleGroupId, "UPDATE");

		if (_log.isDebugEnabled()) {
			_log.debug("hasPermission " + hasPermission);
		}

		if ((articleGroupId > 0) && Validator.isNotNull(articleId)) {
			if (preview && hasPermission) {
				article = JournalArticleLocalServiceUtil
						.fetchLatestArticle(articleGroupId, articleId,
								WorkflowConstants.STATUS_ANY);
			} else {
				article = JournalArticleLocalServiceUtil.fetchLatestArticle(
						articleGroupId, articleId,
						WorkflowConstants.STATUS_APPROVED);
			}
			try {
				if (article == null) {
					article = JournalArticleLocalServiceUtil.getLatestArticle(
							articleGroupId, articleId,
							WorkflowConstants.STATUS_ANY);
				}

				double version = article.getVersion();

				if (preview && hasPermission) {
					articleDisplay = JournalArticleLocalServiceUtil
							.getArticleDisplay(article, ddmTemplateKey,
									viewMode, languageId, page, xmlRequest,
									themeDisplay);
				} else {
					articleDisplay = JournalContentUtil.getDisplay(
							articleGroupId, articleId, version, ddmTemplateKey,
							viewMode, languageId, themeDisplay, page,
							xmlRequest);
				}
			} catch (Exception e) {
				renderRequest.removeAttribute(WebKeys.JOURNAL_ARTICLE);
				if (preview && hasPermission) {
					articleDisplay = JournalArticleLocalServiceUtil
							.getArticleDisplay(article, ddmTemplateKey,
									viewMode, languageId, page, xmlRequest,
									themeDisplay);
				} else {
					articleDisplay = JournalContentUtil.getDisplay(
							articleGroupId, articleId, ddmTemplateKey,
							viewMode, languageId, themeDisplay, page,
							xmlRequest);
				}
			}
		}

		if (article != null) {
			renderRequest.setAttribute(WebKeys.JOURNAL_ARTICLE, article);
		}

		if (articleDisplay != null) {
			renderRequest.setAttribute(WebKeys.JOURNAL_ARTICLE_DISPLAY,
					articleDisplay);
		} else {
			renderRequest.removeAttribute(WebKeys.JOURNAL_ARTICLE_DISPLAY);
		}

		return "portlet.journal_content.view";
	}

	private static Log _log = LogFactoryUtil
			.getLog(CustomJournalContentViewAction.class);

}