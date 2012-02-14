<%--

    Copyright 2010 Västra Götalandsregionen

      This library is free software; you can redistribute it and/or modify
      it under the terms of version 2.1 of the GNU Lesser General Public
      License as published by the Free Software Foundation.

      This library is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU Lesser General Public License for more details.

      You should have received a copy of the GNU Lesser General Public
      License along with this library; if not, write to the
      Free Software Foundation, Inc., 59 Temple Place, Suite 330,
      Boston, MA 02111-1307  USA


--%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>

<style type="text/css">
    .all-account-settings td {
        padding: 15px 40px 0px 10px;
        vertical-align: bottom;
    }

    .all-account-settings .tab-content {
        position: absolute;
    }

    .all-account-settings #tab-content-container {
        height: 250px;
        padding: 10px;
    }

    .all-account-settings input[type="text"] {
        width: 200px;
    }

    .account-settings-form {
        height: 25px;
        text-align: center;
    }

    .tab-selector {
        height: 22px;
        text-align: center;
    }

    .tab-selector span {
        float: left;
        height: 100%;
        margin: 4px;
        width: 31%;
        background-color: #06579c;
        border-top-left-radius: 4px;
        border-top-right-radius: 4px;
        border: 1px solid #75A1C6;
        border-bottom: 0;
        font-family: Ubuntu, Arial, Verdana, Helvetica, sans-serif;
        font-size: 15px;
        font-weight: normal;
        color: white;
    }

    .tab-selector span.selected {
        background-color: white;
        color: #06579C;
    }

</style>

<link href="${pageContext.request.contextPath}/css/style.css" type="text/css" rel="stylesheet"/>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/account-settings.js"></script>

<portlet:actionURL var="saveGeneralActionUrl">
    <portlet:param name="action" value="saveGeneral"/>
</portlet:actionURL>
<portlet:actionURL var="saveEmailActionUrl">
    <portlet:param name="action" value="saveEmail"/>
</portlet:actionURL>
<portlet:actionURL var="savePasswordActionUrl">
    <portlet:param name="action" value="savePassword"/>
</portlet:actionURL>

<p>Blabla...</p>

<div class="all-account-settings">

    <c:if test="${not empty errorMessage}">
        <div class="portlet-msg-error">
                ${errorMessage}
        </div>
    </c:if>

    <div class="tab-selector">
        <span id="span1">Allmänt</span>
        <span id="span2">E-post</span>
        <span id="span3">Lösenord</span>
    </div>
    <div id="tab-content-container">
        <div id="tab1" class="tab-content">
            <table>
                <tbody>
                <aui:form cssClass="account-settings-form" action="${saveGeneralActionUrl}" method="post">
                    <tr>
                        <td><aui:input label="Förnamn: " name="firstName" value="${firstName}"/></td>
                        <td><aui:input label="Telefon: " name="phone" value="${phone}"/></td>
                    </tr>
                    <tr>
                        <td><aui:input label="Mellannamn: " name="middleName" value="${middleName}"/></td>
                        <td><aui:input label="Mobil: " name="mobile" value="${mobile}"/></td>
                    </tr>
                    <tr>
                        <td><aui:input label="Efternamn: " name="lastName" value="${lastName}"/></td>
                        <td><aui:input label="Organisation: " name="organization" value="${organization}"/></td>
                    </tr>
                    <tr>
                        <td></td>
                        <td><input type="submit" value="Spara"/></td>
                    </tr>

                </aui:form>
                </tbody>
            </table>
        </div>

        <div id="tab2" class="tab-content">
            <table>
                <tbody>
                <aui:form cssClass="account-settings-form" action="${saveEmailActionUrl}" method="post">
                    <tr>
                        <td>Nuvarande e-post: <b>${email}</b></td>
                    </tr>
                    <tr>
                        <td><aui:input label="Ny e-post: " name="newEmail" value="${newEmail}"/></td>
                    </tr>
                    <tr>
                        <td><aui:input label="Bekräfta e-post: " name="confirmEmail" value="${confirmEmail}"
                                autocomplete="off"/></td>
                    </tr>
                    <tr>
                        <td><input type="submit" value="Spara"/></td>
                    </tr>
                </aui:form>
                </tbody>
            </table>
        </div>

        <div id="tab3" class="tab-content">
            <table>
                <tbody>
                <aui:form cssClass="account-settings-form" action="${savePasswordActionUrl}" method="post">
                    <tr>
                        <td><aui:input type="password" label="Nytt lösenord: " name="newPassword"
                                       value="${newPassword}"/></td>
                    </tr>
                    <tr>
                        <td><aui:input type="password" label="Bekräfta lösenord: " name="confirmPassword"
                                       value="${confirmPassword}"/></td>
                    </tr>
                    <tr>
                        <td><input type="submit" value="Spara"/></td>
                    </tr>
                </aui:form>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script type="text/javascript">

    selectTab(${selectedTab != null ? selectedTab : 1});

</script>