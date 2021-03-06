package com.intrbiz.bergamot.ui.router.agent;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.UUID;

import com.intrbiz.balsa.engine.route.Router;
import com.intrbiz.balsa.error.http.BalsaNotFound;
import com.intrbiz.balsa.metadata.WithDataAdapter;
import com.intrbiz.bergamot.agent.config.BergamotAgentCfg;
import com.intrbiz.bergamot.agent.config.CfgParameter;
import com.intrbiz.bergamot.crypto.util.CertificatePair;
import com.intrbiz.bergamot.crypto.util.CertificateRequest;
import com.intrbiz.bergamot.crypto.util.PEMUtil;
import com.intrbiz.bergamot.crypto.util.SerialNum;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.metadata.GetBergamotSite;
import com.intrbiz.bergamot.metadata.IsaObjectId;
import com.intrbiz.bergamot.model.AgentRegistration;
import com.intrbiz.bergamot.model.AgentTemplate;
import com.intrbiz.bergamot.model.Config;
import com.intrbiz.bergamot.model.Contact;
import com.intrbiz.bergamot.model.Site;
import com.intrbiz.bergamot.ui.BergamotApp;
import com.intrbiz.metadata.Any;
import com.intrbiz.metadata.CheckStringLength;
import com.intrbiz.metadata.Get;
import com.intrbiz.metadata.Param;
import com.intrbiz.metadata.Post;
import com.intrbiz.metadata.Prefix;
import com.intrbiz.metadata.RequirePermission;
import com.intrbiz.metadata.RequireValidPrincipal;
import com.intrbiz.metadata.Template;

@Prefix("/agent")
@Template("layout/main")
@RequireValidPrincipal()
@RequirePermission("ui.sign.agent")
@RequirePermission("sign.agent")
public class AgentRouter extends Router<BergamotApp>
{    
    @Any("/")
    @WithDataAdapter(BergamotDB.class)
    public void listAgents(BergamotDB db, @GetBergamotSite() Site site)
    {
        model("agents", db.listAgentRegistrations(site.getId()));
        model("agentTemplates", db.listAgentTemplates(site.getId()));
        encode("agent/index");
    }
    
    @Get("/generate-template")
    @WithDataAdapter(BergamotDB.class)
    public void showGenerateAgentTemplate(BergamotDB db, @GetBergamotSite() Site site)
    {
        model("hostTemplates", db.listHostTemplatesWithoutCertificates(site.getId()));
        encode("agent/generate-template");
    }
    
    @Get("/generate-config")
    @RequirePermission("ui.generate.agent")
    public void showGenerateAgentConfig()
    {
        encode("agent/generate-config");
    }
    
    @Get("/sign")
    public void showSignAgentConfig()
    {
        encode("agent/sign-agent");
    }
    
    @Get("/show-template/:id")
    @WithDataAdapter(BergamotDB.class)
    public void showGenerateAgentTemplate(BergamotDB db, @GetBergamotSite() Site site, @IsaObjectId() UUID id)
    {
        // get the template
        AgentTemplate template = db.getAgentTemplate(id);
        // get the certs
        Certificate     rootCert = action("get-root-ca");
        Certificate     siteCert = action("get-site-ca", site.getId());
        // build the config
        BergamotAgentCfg cfg = new BergamotAgentCfg();
        cfg.setCaCertificate(padCert(PEMUtil.saveCertificate(rootCert)));
        cfg.setSiteCaCertificate(padCert(PEMUtil.saveCertificate(siteCert)));
        cfg.setCertificate(padCert(template.getCertificate()));
        cfg.setKey(padCert(template.getPrivateKey()));
        cfg.setName(template.getName());
        cfg.addParameter(new CfgParameter("template-id", null, null, id.toString()));
        // display
        var("agentConfig", cfg.toString() + "\n<!-- Template: UUID=" + id + " CN=" + template.getName() + " -->");
        encode("agent/generated-template");
    }
    
    @Post("/generate-template")
    @WithDataAdapter(BergamotDB.class)
    public void generateTemplate(BergamotDB db, @GetBergamotSite() Site site, @Param("template") @IsaObjectId() UUID templateId)
    {
        // get the template
        Config hostTemplate = notNull(db.getConfig(templateId), "Invalid host template");
        if (! "host".equals(hostTemplate.getType())) throw new BalsaNotFound("Invalid host template");
        // generate
        Certificate     rootCert = action("get-root-ca");
        Certificate     siteCert = action("get-site-ca", site.getId());
        CertificatePair pair     = action("generate-template", site.getId(), hostTemplate, ((Contact) currentPrincipal()).getId());
        // build the config
        BergamotAgentCfg cfg = new BergamotAgentCfg();
        cfg.setCaCertificate(padCert(PEMUtil.saveCertificate(rootCert)));
        cfg.setSiteCaCertificate(padCert(PEMUtil.saveCertificate(siteCert)));
        cfg.setCertificate(padCert(pair.getCertificateAsPEM()));
        cfg.setKey(padCert(pair.getKeyAsPEM()));
        cfg.setName(hostTemplate.getName());
        cfg.addParameter(new CfgParameter("template-id", null, null, templateId.toString()));
        // store the agent registration
        db.setAgentTemplate(new AgentTemplate(site.getId(), templateId, hostTemplate.getName(), hostTemplate.getSummary(), SerialNum.fromBigInt(pair.getCertificate().getSerialNumber()).toString(), pair.getCertificateAsPEM(), pair.getKeyAsPEM()));
        // display
        var("agentConfig", cfg.toString() + "\n<!-- Template: UUID=" + templateId + " CN=" + hostTemplate.getName() + " -->");
        encode("agent/generated-template");
    }
    
    @Post("/generate-config")
    @RequirePermission("ui.generate.agent")
    @WithDataAdapter(BergamotDB.class)
    public void generateAgentConfig(BergamotDB db, @GetBergamotSite() Site site, @Param("common-name") @CheckStringLength(min = 1, max = 255, mandatory = true) String commonName)
    {
        // is an agent already registered
        AgentRegistration existingAgent = db.getAgentRegistrationByName(site.getId(), commonName);
        // assign id
        UUID agentId = var("agentId", existingAgent != null ? existingAgent.getId() : Site.randomId(site.getId()));
        var("commonName", commonName);
        // generate
        Certificate     rootCert = action("get-root-ca");
        Certificate     siteCert = action("get-site-ca", site.getId());
        CertificatePair pair     = action("generate-agent", site.getId(), agentId, commonName, ((Contact) currentPrincipal()).getId());
        // build the config
        BergamotAgentCfg cfg = new BergamotAgentCfg();
        cfg.setCaCertificate(padCert(PEMUtil.saveCertificate(rootCert)));
        cfg.setSiteCaCertificate(padCert(PEMUtil.saveCertificate(siteCert)));
        cfg.setCertificate(padCert(pair.getCertificateAsPEM()));
        cfg.setKey(padCert(pair.getKeyAsPEM()));
        cfg.setName(commonName);
        cfg.addParameter(new CfgParameter("agent-id", null, null, agentId.toString()));
        // store the agent registration
        db.setAgentRegistration(new AgentRegistration(site.getId(), agentId, commonName, SerialNum.fromBigInt(pair.getCertificate().getSerialNumber()).toString()));
        // display
        var("agentConfig", cfg.toString() + "\n<!-- Agent: UUID=" + agentId + " CN=" + commonName + " -->");
        encode("agent/generated-config");
    }
    
    @Post("/sign")
    @WithDataAdapter(BergamotDB.class)
    public void signAgent(BergamotDB db, @GetBergamotSite() Site site, @Param("certificate-request") @CheckStringLength(min = 1, max = 16384, mandatory = true) String certReq) throws IOException
    {
        // parse the certificate request
        CertificateRequest req = PEMUtil.loadCertificateRequest(certReq);
        // is an agent already registered
        AgentRegistration existingAgent = db.getAgentRegistrationByName(site.getId(), req.getCommonName());
        // generate agent it
        UUID agentId = var("agentId", existingAgent != null ? existingAgent.getId() : Site.randomId(site.getId()));
        // sign
        Certificate rootCrt  = action("get-root-ca");
        Certificate siteCrt  = action("get-site-ca", site.getId());
        Certificate agentCrt = action("sign-agent", site.getId(), agentId, req, ((Contact) currentPrincipal()).getId());
        // store the registration
        db.setAgentRegistration(new AgentRegistration(site.getId(), agentId, req.getCommonName(), SerialNum.fromBigInt(((X509Certificate) agentCrt).getSerialNumber()).toString()));
        // display
        var("agentCrt",  PEMUtil.saveCertificate(agentCrt));
        var("siteCaCrt", PEMUtil.saveCertificate(siteCrt));
        var("caCrt",     PEMUtil.saveCertificate(rootCrt));
        encode("agent/signed-agent");
    }
    
    @Post("/revoke")
    @WithDataAdapter(BergamotDB.class)
    public void revokeAgent(BergamotDB db, @GetBergamotSite() Site site, @Param("id") @IsaObjectId() UUID agentId) throws Exception
    {
        // lookup the agent registration
        AgentRegistration agent = db.getAgentRegistration(agentId);
        if (agent != null)
        {
            agent.setRevoked(true);
            agent.setRevokedOn(new Timestamp(System.currentTimeMillis()));
            db.setAgentRegistration(agent);
            // TODO: we should publish a CRL to the agent workers and force a disconnect
        }
        // encode the index
        redirect(path("/agent/"));
    }
    
    public static String padCert(String cert)
    {
        StringBuilder sb = new StringBuilder("\r\n");
        for (String s : cert.split("\n"))
        {
            sb.append("        ").append(s).append("\n");
        }
        //sb.append("\n");
        return sb.toString();
    }
}
