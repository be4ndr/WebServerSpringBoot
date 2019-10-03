package WebServer;

import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSExpressionMemberTermURI;
import com.helger.css.decl.CSSImportRule;
import com.helger.css.decl.ICSSTopLevelRule;
import com.helger.css.decl.visit.ICSSUrlVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CSSUrlVisitor implements ICSSUrlVisitor {
    private String baseUri;
    private String originUri;

    public CSSUrlVisitor(String baseUri, String originUri) {

        this.baseUri = baseUri;
        this.originUri = originUri;
    }

    @Override
    public void onImport(@Nonnull final CSSImportRule aImportRule) {
        // Get the source URL
        final String sURI = aImportRule.getLocationString();
        // Modify the URL
        final String sModifiedURI = UrlUtils.modifyURI(baseUri, originUri, sURI);
        // And set the new URL
        aImportRule.setLocationString(sModifiedURI);
    }

    @Override
    public void onUrlDeclaration(@Nullable final ICSSTopLevelRule aTopLevelRule,
                                 @Nonnull final CSSDeclaration aDeclaration,
                                 @Nonnull final CSSExpressionMemberTermURI aExprTerm) {
        // Get the source URL
        final String sURI = aExprTerm.getURIString();
        // Modify the URL
        final String sModifiedURI = UrlUtils.modifyURI(baseUri, originUri, sURI);
        // And set the new URL
        aExprTerm.setURIString(sModifiedURI);
    }

}
