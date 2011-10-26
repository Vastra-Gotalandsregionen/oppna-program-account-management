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
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<style type="text/css">
    #changePasswordTable tr td {
        padding: 4px;
    }

    #changePasswordTable .borderRow td {
        border: 1px solid #ccc;
    }
</style>

<portlet:actionURL var="changePasswordAction">
    <portlet:param name="action" value="changePassword"/>
</portlet:actionURL>

<c:if test="${not empty errorMessage}">
    <div class="portlet-msg-error">
        ${errorMessage}
    </div>
</c:if>

<form action="${changePasswordAction}" method="POST">
    <table id="changePasswordTable" style="width: auto;">
        <tr class="borderRow">
            <td>VGR-ID:</td>
            <td>${vgrId}</td>
        </tr>
        <tr class="borderRow">
            <td>Lösenord:</td>
            <td><input type="password" name="password" autocomplete="off"/></td>
        </tr>
        <tr class="borderRow">
            <td>Bekräfta lösenord:</td>
            <td><input type="password" name="passwordConfirm" autocomplete="off"/></td>
        </tr>
        <tr>
            <td colspan="2" style="text-align: right;"><input type="submit" value="Ändra lösenord"/></td>
        </tr>
    </table>
</form>