-- Seed domains
INSERT INTO domains (name, description) VALUES
    ('Technology', 'Information technology and software services'),
    ('Healthcare', 'Healthcare, life sciences, and medical devices'),
    ('Finance', 'Financial services, banking, and capital markets'),
    ('Retail', 'Retail, consumer goods, and e-commerce'),
    ('Manufacturing', 'Manufacturing, industrial, and supply chain'),
    ('Energy', 'Energy, oil and gas, and utilities'),
    ('Education', 'Education, e-learning, and academic institutions'),
    ('Government', 'Government, public sector, and defense'),
    ('Telecom', 'Telecommunications and networking'),
    ('Media', 'Media, entertainment, and publishing');

-- Seed industries
INSERT INTO industries (name, description) VALUES
    ('Banking', 'Commercial and retail banking services'),
    ('Insurance', 'Life, health, and property insurance'),
    ('Pharma', 'Pharmaceutical and biotechnology'),
    ('E-commerce', 'Online retail and marketplace platforms'),
    ('Automotive', 'Automotive manufacturing and mobility'),
    ('Oil & Gas', 'Oil exploration, refining, and distribution'),
    ('Higher Education', 'Universities, colleges, and research institutions'),
    ('Federal', 'Federal government agencies and departments'),
    ('Wireless', 'Wireless telecommunications and mobile networks'),
    ('Broadcasting', 'Television, radio, and digital broadcasting');

-- Seed technologies
INSERT INTO technologies (name, description) VALUES
    ('Java', 'Java / Spring Boot enterprise development'),
    ('Python', 'Python development including Django and FastAPI'),
    ('.NET', 'Microsoft .NET framework and .NET Core'),
    ('React', 'React.js frontend framework'),
    ('Angular', 'Angular frontend framework'),
    ('AWS', 'Amazon Web Services cloud platform'),
    ('Azure', 'Microsoft Azure cloud platform'),
    ('Salesforce', 'Salesforce CRM and platform development'),
    ('SAP', 'SAP ERP and enterprise solutions'),
    ('Microservices', 'Microservices architecture and design patterns'),
    ('AI/ML', 'Artificial intelligence and machine learning'),
    ('DevOps', 'DevOps practices, CI/CD, and infrastructure as code'),
    ('Kubernetes', 'Container orchestration with Kubernetes'),
    ('Power BI', 'Microsoft Power BI analytics and reporting'),
    ('Snowflake', 'Snowflake cloud data warehouse');

-- Seed document types
INSERT INTO document_types (name, description) VALUES
    ('Case Study', 'Client case study documenting project outcomes and impact'),
    ('White Paper', 'Technical white paper on industry trends or solutions');

-- Seed business units
INSERT INTO business_units (name, description) VALUES
    ('Digital Engineering', 'Custom software development and digital transformation'),
    ('Data Analytics', 'Data engineering, analytics, and business intelligence'),
    ('Cloud Services', 'Cloud migration, infrastructure, and managed services'),
    ('Enterprise Applications', 'ERP, CRM, and enterprise platform implementations');

-- Seed SBUs (Strategic Business Units / Regions)
INSERT INTO sbus (name, description) VALUES
    ('North America', 'United States and Canada operations'),
    ('Europe', 'European operations including UK and EU'),
    ('APAC', 'Asia-Pacific operations'),
    ('India', 'India domestic operations');
