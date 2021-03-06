package com.intrbiz.bergamot.ui.api;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.intrbiz.balsa.engine.route.Router;
import com.intrbiz.balsa.metadata.WithDataAdapter;
import com.intrbiz.bergamot.crypto.util.CertificateRequest;
import com.intrbiz.bergamot.crypto.util.PEMUtil;
import com.intrbiz.bergamot.crypto.util.SerialNum;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.metadata.IgnoreBinding;
import com.intrbiz.bergamot.metadata.IsaObjectId;
import com.intrbiz.bergamot.model.AgentRegistration;
import com.intrbiz.bergamot.model.Contact;
import com.intrbiz.bergamot.model.Site;
import com.intrbiz.bergamot.ui.BergamotApp;
import com.intrbiz.metadata.Any;
import com.intrbiz.metadata.CheckStringLength;
import com.intrbiz.metadata.JSON;
import com.intrbiz.metadata.Param;
import com.intrbiz.metadata.Prefix;
import com.intrbiz.metadata.RequirePermission;
import com.intrbiz.metadata.RequireValidPrincipal;
import com.intrbiz.metadata.Var;
import com.intrbiz.metadata.doc.Desc;
import com.intrbiz.metadata.doc.Title;


@Title("Agent API Methods")
@Desc({
    "The Bergamot Agent runs on servers and allows checks to be run locally on a host."
})
@Prefix("/api/agent")
@RequireValidPrincipal()
@RequirePermission("api.sign.agent")
@RequirePermission("sign.agent")
public class AgentAPIRouter extends Router<BergamotApp>
{    
    /**
     * Sign an agent certificate request
     */
    @Title("Sign agent")
    @Desc({
        "Sign a Bergamot Agent certificate request by the site certificate authority.  This will return a certificate which can then be installed on the corresponding host."
    })
    @Any("/sign-agent")
    @JSON
    @WithDataAdapter(BergamotDB.class)
    @IgnoreBinding
    public List<String> signAgent(
            BergamotDB db, 
            @Var("site") Site site, 
            @Param("certificate-request") @CheckStringLength(min = 1, max = 16384, mandatory = true) String certReq
    ) throws IOException
    {
        // parse the certificate request
        CertificateRequest req = PEMUtil.loadCertificateRequest(certReq);
        // is an agent already registered
        AgentRegistration existingAgent = db.getAgentRegistrationByName(site.getId(), req.getCommonName());
        // assign the agent UUID
        UUID agentId = var("agentId", existingAgent != null ? existingAgent.getId() : Site.randomId(site.getId()));
        // get the Root CA Certificate
        Certificate rootCrt  = action("get-root-ca");
        // get the Site CA Certificate
        Certificate siteCrt  = action("get-site-ca", site.getId());
        // ok, actually sign the agent certificate
        Certificate agentCrt = action("sign-agent", site.getId(), agentId, req, ((Contact) currentPrincipal()).getId());
        // store the registration
        db.setAgentRegistration(new AgentRegistration(site.getId(), agentId, req.getCommonName(), SerialNum.fromBigInt(((X509Certificate) agentCrt).getSerialNumber()).toString()));
        // return the certificate chain
        return Arrays.asList(new String[] {
                PEMUtil.saveCertificate(agentCrt),
                PEMUtil.saveCertificate(siteCrt),
                PEMUtil.saveCertificate(rootCrt),
        });
    }
    
    /**
     * Sign an agent key
     */
    @Title("Sign agent key")
    @Desc({
        "Sign a Bergamot Agent public key generating a certificate with the given common name.  The resulting certificate can be installed on the corresponding host."
    })
    @Any("/sign-agent-key")
    @JSON
    @WithDataAdapter(BergamotDB.class)
    @IgnoreBinding
    public List<String> signAgentKey(
            BergamotDB db, 
            @Var("site") Site site,
            @Param("common-name") @CheckStringLength(min = 1, max = 255, mandatory = true)   String commonName,
            @Param("public-key")  @CheckStringLength(min = 1, max = 16384, mandatory = true) String publicKey
    ) throws IOException
    {
        // is an agent already registered
        AgentRegistration existingAgent = db.getAgentRegistrationByName(site.getId(), commonName);
        // decode the key
        PublicKey key = PEMUtil.loadPublicKey(publicKey);
        // assign the agent UUID
        UUID agentId = var("agentId", existingAgent != null ? existingAgent.getId() : Site.randomId(site.getId()));
        // get the Root CA Certificate
        Certificate rootCrt  = action("get-root-ca");
        // get the Site CA Certificate
        Certificate siteCrt  = action("get-site-ca", site.getId());
        // ok, actually sign the agent certificate
        Certificate agentCrt = action("sign-agent-key", site.getId(), agentId, commonName, key, ((Contact) currentPrincipal()).getId());
        // store the registration
        db.setAgentRegistration(new AgentRegistration(site.getId(), agentId, commonName, SerialNum.fromBigInt(((X509Certificate) agentCrt).getSerialNumber()).toString()));
        // return the certificate chain
        return Arrays.asList(new String[] {
                PEMUtil.saveCertificate(agentCrt),
                PEMUtil.saveCertificate(siteCrt),
                PEMUtil.saveCertificate(rootCrt),
        });
    }
    
    /**
     * Revoke an agent certificate
     */
    @Title("Revoke agent")
    @Desc({
        "Revoke the agent identified by the given UUID."
    })
    @Any("/revoke-agent")   
    @JSON
    @WithDataAdapter(BergamotDB.class)
    @IgnoreBinding
    public String revokeAgent(BergamotDB db, @Var("site") Site site, @Param("id") @IsaObjectId UUID agentId) throws IOException
    {
        AgentRegistration agent = notNull(db.getAgentRegistration(agentId), "No such agent: " + agentId);
        agent.setRevoked(true);
        agent.setRevokedOn(new Timestamp(System.currentTimeMillis()));
        db.setAgentRegistration(agent);
        return "revoked";
    }
}
