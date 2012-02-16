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
<portlet:renderURL var="toTab1">
    <portlet:param name="tab" value="1"/>
</portlet:renderURL>
<portlet:renderURL var="toTab2">
    <portlet:param name="tab" value="2"/>
</portlet:renderURL>
<portlet:renderURL var="toTab3">
    <portlet:param name="tab" value="3"/>
</portlet:renderURL>

<div class="account-settings-portlet">

    <c:if test="${not empty successMessage}">
        <div class="portlet-msg-success">
                ${successMessage}
        </div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="portlet-msg-error">
                ${errorMessage}
        </div>
    </c:if>

    <div class="tab-selector">
        <a id="anchor1" href="${toTab1}" class="${selectedTab == 1 ? 'selected' : 'not-selected'}"><span id="span1">Allmänt</span></a>
        <a id="anchor2" href="${toTab2}" class="${selectedTab == 2 ? 'selected' : 'not-selected'}"><span id="span2">E-post</span></a>
        <a id="anchor3" href="${toTab3}" class="${selectedTab == 3 ? 'selected' : 'not-selected'}"><span id="span3">Lösenord</span></a>
    </div>
    <div id="tab-content-container">

        <div id="tab1" class="tab-content" style="${selectedTab == 1 ? 'visibility: visible' : 'visibility: hidden'}">
            <table>
                <tbody>
                <aui:form cssClass="account-settings-form" action="${saveGeneralActionUrl}" method="post">
                    <tr>
                        <td><aui:input label="Förnamn: " name="firstName" value="${firstName}"/></td>
                        <td><aui:input label="Telefon: " name="telephone" value="${telephone}"/></td>
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

        <div id="tab2" class="tab-content" style="${selectedTab == 2 ? 'visibility: visible' : 'visibility: hidden'}">
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

        <div id="tab3" class="tab-content" style="${selectedTab == 3 ? 'visibility: visible' : 'visibility: hidden'}">
            <table>
                <tbody>
                <aui:form cssClass="account-settings-form" action="${savePasswordActionUrl}" method="post">
                    <tr>
                        <td>
                            Lösenordet måste vara minst 6 tecken långt, innehålla både siffror och bokstäver och bara siffror och bokstäver (ej åäö).
                        </td>
                    </tr>
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