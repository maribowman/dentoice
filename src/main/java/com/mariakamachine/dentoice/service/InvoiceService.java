package com.mariakamachine.dentoice.service;

import com.mariakamachine.dentoice.config.properties.InvoiceProperties;
import com.mariakamachine.dentoice.data.entity.CostWrapperEntity;
import com.mariakamachine.dentoice.data.entity.DentistEntity;
import com.mariakamachine.dentoice.data.entity.InvoiceEntity;
import com.mariakamachine.dentoice.data.enums.InsuranceType;
import com.mariakamachine.dentoice.data.jsonb.EffortJsonb;
import com.mariakamachine.dentoice.data.jsonb.MaterialJsonb;
import com.mariakamachine.dentoice.data.repository.InvoiceRepository;
import com.mariakamachine.dentoice.exception.NotFoundException;
import com.mariakamachine.dentoice.model.FileResource;
import com.mariakamachine.dentoice.rest.dto.Effort;
import com.mariakamachine.dentoice.rest.dto.Invoice;
import com.mariakamachine.dentoice.rest.dto.Material;
import com.mariakamachine.dentoice.util.invoice.pdf.InvoicePdfGenerator;
import com.mariakamachine.dentoice.util.invoice.xml.InvoiceConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static com.mariakamachine.dentoice.util.invoice.xml.XmlGenerator.generateInvoiceXmlFile;
import static java.lang.String.format;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceProperties invoiceProperties;
    private final DentistService dentistService;
    private final EffortService effortService;
    private final MaterialService materialService;

    @Autowired
    public InvoiceService(InvoiceRepository invoiceRepository, InvoiceProperties invoiceProperties, DentistService dentistService, EffortService effortService, MaterialService materialService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceProperties = invoiceProperties;
        this.dentistService = dentistService;
        this.effortService = effortService;
        this.materialService = materialService;
    }

    public InvoiceEntity create(Invoice invoice) {
        return invoiceRepository.save(createInvoiceEntity(invoice));
    }

    public InvoiceEntity getById(long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(format("could not find invoice with [ id: %d ]", id)));
    }

    public List<InvoiceEntity> getAllByDentistId(long id) {
        return invoiceRepository.findAllByDentistIdOrderByDate(id);
    }

    public FileResource getXmlById(Long id) {
//        return generateInvoiceXmlFile(new InvoiceConverter().convertToXmlModel(getById(id), invoiceProperties));
        return generateInvoiceXmlFile(new InvoiceConverter().convertToXmlModel(invoices().get(7), invoiceProperties));
    }

    public FileResource getPdfById(Long id) {
//        return new InvoicePdfGenerator().generatePdf(getById(id));
        return new InvoicePdfGenerator().generatePdf(invoices().get(7), invoiceProperties);
    }

    public FileResource getMonthlyPdf(LocalDate from, LocalDate to, Long dentistId) {
//        DentistEntity dentist = dentistService.getById(dentistId);
        List<InvoiceEntity> invoices = invoiceRepository.findAllByDentistIdAndDateAfterAndDateBeforeOrderByDateAsc(dentistId, from, to);
        return new InvoicePdfGenerator().generateMonthlyPdf(invoices(), invoiceProperties);
    }


    List<InvoiceEntity> invoices() {
        List<InvoiceEntity> invoices = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            InvoiceEntity invoice = new InvoiceEntity();
            invoice.setId(Long.valueOf("1812000" + i));
            invoice.setDate(LocalDate.now());
            invoice.setDescription("24 Tele");
            invoice.setXmlNumber(UUID.randomUUID().toString());
            invoice.setPatient("Patient Nummer " + i);
            invoice.setInsuranceType(InsuranceType.PRIVATE);
            CostWrapperEntity costs = new CostWrapperEntity();

            MaterialJsonb material2 = new MaterialJsonb();
            material2.setPosition("7895");
            material2.setDescription("PlatinLloyd 100");
            material2.setNotes("BEGO Legierung CE ja");
            material2.setQuantity(new BigDecimal(Math.random() * i).setScale(4, BigDecimal.ROUND_HALF_DOWN).doubleValue());
            material2.setPricePerUnit(51.25);
            material2.setMetal(true);

            MaterialJsonb material1 = new MaterialJsonb();
            material1.setPosition("7895");
            material1.setDescription("PlatinLloyd 100");
            material1.setQuantity(3.60);
            material1.setPricePerUnit(51.25);
            costs.setMaterials(Arrays.asList(material1, material2));

            EffortJsonb effort = new EffortJsonb();
            effort.setPosition("1234");
            effort.setDescription("Desinfektion");
            effort.setPricePerUnit(234.43);
            effort.setQuantity(new BigDecimal(Math.random() * i).setScale(4, BigDecimal.ROUND_HALF_DOWN).doubleValue());

            costs.setEfforts(Collections.singletonList(effort));
            invoice.setCosts(costs);
            invoices.add(invoice);

            DentistEntity dentist = new DentistEntity();
            dentist.setTitle("Herr Zahnarzt");
            dentist.setStreet("Leinenweberstr. 47");
            dentist.setLastName("Bims");
            dentist.setZip("70567");
            dentist.setFirstName("Halo I");
            dentist.setCity("Stuttgart");
            invoice.setDentist(dentist);
        }
        return invoices;
    }

    // update

    // delete

    // get all

    private InvoiceEntity createInvoiceEntity(Invoice invoice) {
        InvoiceEntity entity = new InvoiceEntity();
        // get dentist by id
        entity.setDentist(dentistService.getById(invoice.getDentist()));
        entity.setPatient(invoice.getPatient());
        entity.setDescription(invoice.getDescription());
        // TODO check for xml number duplicate for dentist
        entity.setXmlNumber(invoice.getXmlNumber());
        entity.setInvoiceType(invoice.getInvoiceType());
        entity.setInsuranceType(invoice.getInsuranceType());
        entity.setDate(invoice.getDate());
        // complete efforts
        List<EffortJsonb> effortJsonbList = new ArrayList<>();
        for (Effort effort : invoice.getEfforts()) {
            effortJsonbList.add(new EffortJsonb(effort, effortService.getByPosition(effort.getPosition())));
        }
        // complete materials
        List<MaterialJsonb> materialJsonbList = new ArrayList<>();
        for (Material material : invoice.getMaterials()) {
            materialJsonbList.add(new MaterialJsonb(material, materialService.getByPosition(material.getPosition())));
        }
        entity.setCosts(new CostWrapperEntity(effortJsonbList, materialJsonbList));
        return entity;
    }

}
