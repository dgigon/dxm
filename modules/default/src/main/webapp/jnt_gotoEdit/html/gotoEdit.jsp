<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<a href="${url.edit}">
    <c:if test="${!empty currentNode.properties.picto.node}">
        <img src="${currentNode.properties.picto.node.url}" alt="<fmt:message key='label.administration'/>"/>
    </c:if>
    <c:if test="${empty currentNode.properties.picto.node}">
        <img src="${url.context}/icons/editContent.png" width="16" height="16" alt=" " role="presentation" style="position:relative; top: 4px; margin-right:2px; ">
        <c:if test="${!empty currentNode.properties['jcr:title']}">
            ${currentNode.properties["jcr:title"].string}
        </c:if>
        <c:if test="${empty currentNode.properties['jcr:title']}">
            <fmt:message key="label.edit"/>
        </c:if>
    </c:if>
</a>