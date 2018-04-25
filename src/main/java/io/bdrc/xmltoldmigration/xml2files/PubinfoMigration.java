package io.bdrc.xmltoldmigration.xml2files;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;


public class PubinfoMigration {

	private static final String WPXSDNS = "http://www.tbrc.org/models/pubinfo#";
    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;

	// used for testing only
	public static Model MigratePubinfo(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(m, "work");
        Element root = xmlDocument.getDocumentElement();
        Resource main = null;
        
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "isPubInfoFor");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("work");
            if (value.isEmpty()) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "work", "missing work ID!");
                return m;
            }
            main = m.createResource(BDR+value);
        }
        m.add(main, RDF.type, m.getResource(BDO+"Work"));
        MigratePubinfo(xmlDocument, m, main, new HashMap<String,Model>());
        return m;
	}
	
	public static boolean isComputerInputDbuMed(String RID) {
	    switch(RID) {
	    case "W8LS25451":
	    case "W8LS25572":
	    case "W8LS25575":
	    case "W8LS25578":
	    case "W8LS25590":
	    case "W8LS25593":
	    case "W8LS26096":
	    case "W8LS26099":
	    case "W8LS26102":
	    case "W8LS26105":
	    case "W8LS26182":
	    case "W8LS26185":
	        return true;
	    }
	    return false;
	}
	
	// use this giving a wkr:Work as main argument to fill the work data
	public static Model MigratePubinfo(final Document xmlDocument, final Model m, final Resource main, final Map<String,Model> itemModels) {
		Element root = xmlDocument.getDocumentElement();
		
        addSimpleElement("publisherName", BDO+"workPublisherName", "en", root, m, main);
        addSimpleElement("publisherLocation", BDO+"workPublisherLocation", "en", root, m, main);
        addSimpleElement("printery", BDO+"workPrintery", "bo-x-ewts", root, m, main);
        addSimpleDateElement("publisherDate", "PublishedEvent", root, m, main);
        addSimpleElement("lcCallNumber", BDO+"workLcCallNumber", null, root, m, main);
        addSimpleElement("lccn", BDO+"workLccn", null, root, m, main);
        addSimpleElement("hollis", BDO+"workHollis", null, root, m, main);
        addSimpleElement("seeHarvard", BDO+"workSeeHarvard", null, root, m, main);
        addSimpleElement("pl480", BDO+"workPL480", null, root, m, main);
        addSimpleElement("isbn", BDO+"workIsbn", null, root, m, main);
        addSimpleElement("authorshipStatement", BDO+"workAuthorshipStatement", CommonMigration.EWTS_TAG, root, m, main);
        addSimpleDateElement("dateOfWriting", "CompletedEvent", root, m, main);
        addSimpleElement("extent", BDO+"workExtentStatement", null, root, m, main);
        addSimpleElement("illustrations", BDO+"workIllustrations", null, root, m, main);
        addSimpleElement("dimensions", BDO+"workDimensions", null, root, m, main);
        addSimpleElement("volumes", ADM+"workVolumesNote", null, root, m, main);
        addSimpleElement("seriesName", BDO+"workSeriesName", CommonMigration.EWTS_TAG, root, m, main);
        addSimpleElement("seriesNumber", BDO+"workSeriesNumber", null, root, m, main);
        addSimpleElement("biblioNote", BDO+"workBiblioNote", "en", root, m, main);
        addSimpleElement("sourceNote", BDO+"workSourceNote", "en", root, m, main);
        addSimpleElement("editionStatement", BDO+"workEditionStatement", CommonMigration.EWTS_TAG, root, m, main);
        
        // TODO: this goes in the item
        addSimpleElement("tbrcHoldings", BDO+"itemBDRCHoldingStatement", null, root, m, main);
        
        CommonMigration.addNotes(m, root, main, WPXSDNS);
        CommonMigration.addExternals(m, root, main, WPXSDNS);
        CommonMigration.addLog(m, root, main, WPXSDNS);
        
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "series");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("name").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(BDO, "workSeriesName"), m.createLiteral(value));
            value = current.getAttribute("number").trim();
            if (!value.isEmpty()) {
                m.add(main, m.getProperty(BDO, "workSeriesNumber"), m.createLiteral(value));
                m.add(main, m.getProperty(BDO, "workIsNumbered"), m.createTypedLiteral(true));
            }
            Property prop = m.getProperty(BDO, "workSeriesContent");
            Literal l = CommonMigration.getLiteral(current, CommonMigration.EWTS_TAG, m, "series", main.getLocalName(), null);
            if (l == null) continue;
            main.addProperty(prop, l);
            Statement s = main.getProperty(m.getProperty(BDO, "workExpressionOf"));
            if (s != null) {
                l = s.getLiteral();
                main.removeAll(m.getProperty(BDO, "workExpressionOf"));
                main.addProperty(m.getProperty(BDO, "workNumberOf"), l);
            }
        }
        
        nodeList = root.getElementsByTagNameNS(WPXSDNS, "printType");
        boolean langTibetanDone = false;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("type").trim();
            switch(value) {
            case "dbuMed":
                langTibetanDone = true;
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoDbuMed"));
                if (isComputerInputDbuMed(main.getLocalName()))
                    m.add(main, m.getProperty(BDO, "workObjectType"), m.createResource(BDR+"ObjectTypeComputerInput"));
                else
                    m.add(main, m.getProperty(BDO, "workObjectType"), m.createResource(BDR+"ObjectTypeManuscript"));
                break;
            case "dbuCan":
                langTibetanDone = true;
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoDbuCan"));
                break;
            case "blockprint":
                m.add(main, m.getProperty(BDO, "workObjectType"), m.createResource(BDR+"ObjectTypeBlockprint"));
                break;
            case "computerInput":
                m.add(main, m.getProperty(BDO, "workObjectType"), m.createResource(BDR+"ObjectTypeComputerInput"));
                break;
            case "OCR":
                m.add(main, m.getProperty(BDO, "workObjectType"), m.createResource(BDR+"ObjectTypeOCR"));
                break;
            case "typeSet":
                m.add(main, m.getProperty(BDO, "workObjectType"), m.createResource(BDR+"ObjectTypeTypeSet"));
                break;
            case "facsimile":
                m.add(main, m.getProperty(BDO, "workObjectType"), m.createResource(BDR+"ObjectTypeFacsimile"));
                break;
            default:
                break;
            }
        }
        
        nodeList = root.getElementsByTagNameNS(WPXSDNS, "encoding");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getTextContent().trim();
            if (value.isEmpty()) continue;
            value = value.trim().toLowerCase();
            switch (value) {
            case "in tibetan":
            case "in tibetan.":
            case "བོད་ཡིག":
            case "ྦོབོད་ཡིག":
            case "ྦབོད་ཡིག":
            case " ྐབོད་ཡིག":
            case "ྦོོབོད་ཡིག":
            case "བོ་དཡིག":
            case "ཡིག":
            case "ྐབོད་ཡིག":
            case "བོད་ཡི":
            case "བོད་ཡིངག":
            case "ྦོད་ཡིག":
            case "བོད་སྐད།":
            case "བིད་ཡིག":
            case "བོད་ཡིབ":
            case "བོད་ཡོག":
            case "བོདཡིག":
            case "བོད":
            case "བོད་":
            case "བོད་ཡིག་":
            case "བ་ོད་ཡིག":
            case "བོག་ཡིག":
            case "ྦིབོད་ཡིག":
            case "བོད་ཡིག༌":
            case "ོབོད་ཡིག":
            case "བོད་རིགས།":
            case "བོང་ཡིག":
            case "in tibetab":
            case "inntibetan":
            case "intibetan":
            case "in tibet":
            case "inn tibetan":
            case "in tibatan":
            case "ln tibetan":
            case "in tibean":
            case "in tibeta":
            case "in tibetabn":
            case "in toibetan":
            case "in tbetan":
            case "in tibetyan":
            case "in ttibetan":
            case "in tibeatan":
            case "in tebe":
            case "in tibetan;":
            case "in tibeatn":
            case "tibetan":
            case "in tibtan":
            case "im tibetan":
            case "in tiibetan":
            case "in titeian":
            case "in  tibetan":
            case "in་tibetan":
            case "in tibat":
            case "in tietan":
            case "oin tibetan":
            case "in tobetan":
            case "in ti betan":
            case "in tidetan":
            case "un tibetan":
            case "in tiobetan":
            case "ni tibetan":
            case "in tibtatan":
                if (!langTibetanDone)
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoTibt"));
                break;
            case "extendedwylie":
            case "estended wylie":
            case "extended wylie":
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoEwts"));
                break;
            case "in dzongkha":
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"DzTibt"));
                break;
            case "བོད་དབྱིན།":
            case "དབྱིན་ཡིག":
            case "བོད་ཡིག  དབྱིན་ཡིག":
            case "བོད་དབྱིན":
            case "དབྱིན་བོད།":
            case "བོད་ཡིག english":
            case "in tibetan & english":
            case "in tibetan and english":
            case "in english and tibetan":
            case "in tibean & english":
            case "tibetan and english":
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"EnLatn")); // TODO
                if (!langTibetanDone)
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoTibt"));
                break;
            case "in chinese":
            case "in chinece":
            case "chinese":
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"EnLatn")); // TODO
                break;
            case "in chinese & tibetan":
            case "in tibetan and chinese":
            case "in chinese and tibetan":
            case "in tibetan & chinese":
            case "in tibetan and chinise":
            case "in tibetan with chinese":
            case "in tibetan and chinece":
            case "in tibetan and chinses":
            case "in tibetan with chinece":
            case "in chinese，tibetan":
            case "in chinese in tibetan":
            case "in tibetan chinese":
            case "tobetan with chinece":
            case "in tibetab with chinece":
                if (!langTibetanDone)
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoTibt"));
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"ZhUnknown"));
                break;
            case "in sanskrit":
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"SaUnknown"));
                break;
            case "བོད་ཡིག་དང་རྒྱ་ཡིག།":
            case "in sanskrit & tibetan":
            case "in sanskrit and tibetan":
            case "in tibetan and sanskrit":
            case "in tibetan & sanskrit":
                if (!langTibetanDone)
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoTibt"));
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"SaUnknown"));
                break;
            case "in mongolian":
            case "mongolian":
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"MnUnknown"));
                break;
            case "in tibetan and mongol":
            case "in tibetan and mongolian":
            case "in mongolian and tibetan":
                if (!langTibetanDone)
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoTibt"));
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"MnUnknown"));
                break;
            case "english":
            case "in english":
            case "en":
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"EnLatn"));
                break;
            case "in tibetan, english and chinese":
            case "in chinese, tibetan and english":
            case "in tibetan, chinese & english":
            case "in tibetan, chinece and english":
            case "tibetan, english and chinese":
            case "in tibetan chinese english":
            case "in tibetan, chinese and english":
            case "in chinese, english and tibetan":
            case "in english, tibetan and chinese":
                if (!langTibetanDone)
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoTibt"));
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"EnLatn"));
                m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"ZhUnknown"));
                break;
            case "in tibetan; an excerpt in english":
            case "in tibetan; notes in english":
            case "in tibetan; preface in english":
            case "in tibetan; pref. in english":
            case "in tibetan, preface in english":
            case "in tibetan; prefatory in english":
            case "in tibetan; publisher's note in english":
            case "in tibetan; includes english terms":
            case "in tibetan; introduction in english":
            case "introduction in english":
            case "in tibetan; brief biography of author in english":
            case "in tibetan; preface and acknowledge in english":
            case "in tibetan; prologue and acknowledgements in tibetan and english":
                if (!langTibetanDone)
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoTibt"));
                m.add(main, m.getProperty(BDO, "workOtherLangScript"), m.createResource(BDR+"EnLatn"));
                break;
            default:
                boolean langFound = false;
                if (value.contains("chinese")) {
                    langFound = true;
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"ZhUnknown"));
                }
                if (value.contains("english") || value.contains("དབྱིན") || value.contains("ཨིན")) {
                    langFound = true;
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"EnLatn"));
                }
                if (value.contains("mongol")) {
                    langFound = true;
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"MnUnknown"));
                }
                if (value.contains("tibet") || value.contains("བོད")) {
                    langFound = true;
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoTibt"));
                }
                if (value.contains("sanskrit") || value.contains("རྒྱ")) {
                    langFound = true;
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"SaUnknown"));
                }
                if (value.contains("dzongkha")) {
                    langFound = true;
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"DzTibt"));
                }
                if (value.contains("hindi")) {
                    langFound = true;
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"HiUnknown"));
                }
//                if (!langFound)
//                    System.out.println(main.getLocalName()+" "+value);
                // TODO: migration exception: add initial string
                break;
            }
        }

        nodeList = root.getElementsByTagNameNS(WPXSDNS, "sourcePrintery");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("place").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(BDO, "workHasSourcePrintery"), m.createResource(BDR+value));
            else {
                value = current.getTextContent().trim();
                if (!value.isEmpty()) {
                    m.add(main, m.getProperty(BDO, "workSourcePrintery_string"), m.createLiteral(value));
                } else {
                    ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "sourcePrintery", "missing source printery ID!");
                }
            }
        }
        
        nodeList = root.getElementsByTagNameNS(WPXSDNS, "holding");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String itemName = "I"+main.getLocalName().substring(1)+"_P"+String.format("%03d", i+1);
            Model itemModel = m;
            if (WorkMigration.splitItems) {
                itemModel = ModelFactory.createDefaultModel();
                CommonMigration.setPrefixes(itemModel, "item");
                itemModels.put(itemName, itemModel);
            }
            Resource holding = itemModel.createResource(BDR+itemName);
            itemModel.add(holding, RDF.type, itemModel.getResource(BDO+"ItemPhysicalAsset"));
            if (WorkMigration.addItemForWork)
                itemModel.add(holding, itemModel.createProperty(BDO, "itemForWork"), itemModel.createResource(main.getURI()));
            if (WorkMigration.addWorkHasItem) {
                m.add(main, m.getProperty(BDO, "workHasItemPhysicalAsset"), m.createResource(BDR+itemName));
            }

            addSimpleElement("exception", BDO+"itemException", CommonMigration.EWTS_TAG, current, itemModel, holding);
            String value;
            NodeList subNodeList = root.getElementsByTagNameNS(WPXSDNS, "shelf");
            for (int j = 0; j < subNodeList.getLength(); j++) {
                Element subCurrent = (Element) subNodeList.item(j);
                value = subCurrent.getTextContent().trim();
                if (!value.isEmpty())
                    itemModel.add(holding, itemModel.createProperty(BDO, "itemShelf"), itemModel.createLiteral(value));
                
                value = subCurrent.getAttribute("copies").trim();
                if (!value.isEmpty())
                    itemModel.add(holding, itemModel.createProperty(BDO, "itemCopies"), itemModel.createLiteral(value));
            }
            
            subNodeList = root.getElementsByTagNameNS(WPXSDNS, "library");
            for (int j = 0; j < subNodeList.getLength(); j++) {
                Element subCurrent = (Element) subNodeList.item(j);
                value = subCurrent.getAttribute("rid").trim();
                if (!value.isEmpty())
                    itemModel.add(holding, itemModel.createProperty(BDO, "itemLibrary"), itemModel.createResource(BDR+value));
                else
                    ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "holding", "Pubinfo holding has no library RID!");
                
                // ignore @code and content
            }
        }
		return m;
	}

	public static void addSimpleElement(String elementName, String propName, String defaultLang, Element root, Model m, Resource main) {
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, elementName);
        String rid = root.getAttribute("RID");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = null;
            if (defaultLang != null) {
                Property prop = m.createProperty(propName);
                Literal l = CommonMigration.getLiteral(current, defaultLang, m, elementName, rid, null);
                if (l != null)
                    main.addProperty(prop, l);
            } else {
                value = current.getTextContent().trim();
                if (value.isEmpty()) return;
                m.add(main, m.createProperty(propName), m.createLiteral(value));
            }
        }
    }

    public static void addSimpleDateElement(String elementName, String eventType, Element root, Model m, Resource main) {
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, elementName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = null;
            value = current.getTextContent().trim();
            if (value.isEmpty()) return;
            CommonMigration.addDatesToEvent(value, main, "workEvent", eventType);
        }
    }
	
}
